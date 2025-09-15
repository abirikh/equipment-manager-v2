package bot;

import model.Equipment;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import qr.QrReader;
import repository.EquipmentRepository;
import repository.SQLiteEquipmentRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final EquipmentRepository repository;

    // --- Для пошагового редактирования ---
    private enum BotState {
        NONE,
        WAITING_FOR_EQUIPMENT_FOR_DESC,
        WAITING_FOR_NEW_DESC,
        WAITING_FOR_EQUIPMENT_FOR_LOC,
        WAITING_FOR_NEW_LOC
    }

    private final Map<Long, BotState> userStates = new HashMap<>();
    private final Map<Long, Integer> tempEquipmentId = new HashMap<>();


    public EquipmentBot(String token, String username) {
        this.botToken = token;
        this.botUsername = username;
        this.repository = new SQLiteEquipmentRepository(); // ✅ используем новый репозиторий
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();


// --- Обработка кнопок ---
            if (text.equals("Изменить описание")) {
                sendText(chatId, "Введите ID или название оборудования:");
                userStates.put(chatId, BotState.WAITING_FOR_EQUIPMENT_FOR_DESC);
                return;
            }

            if (text.equals("Изменить расположение")) {
                sendText(chatId, "Введите ID или название оборудования:");
                userStates.put(chatId, BotState.WAITING_FOR_EQUIPMENT_FOR_LOC);
                return;
            }

// --- Проверяем состояние пользователя ---
            BotState state = userStates.getOrDefault(chatId, BotState.NONE);

            switch (state) {
                case WAITING_FOR_EQUIPMENT_FOR_DESC -> {
                    Equipment eq = findEquipmentByText(text);
                    if (eq == null) {
                        sendText(chatId, "Оборудование не найдено. Попробуйте ещё раз.");
                    } else {
                        tempEquipmentId.put(chatId, eq.getId());
                        sendText(chatId, "Введите новое описание:");
                        userStates.put(chatId, BotState.WAITING_FOR_NEW_DESC);
                    }
                    return;
                }

                case WAITING_FOR_NEW_DESC -> {
                    Integer id = tempEquipmentId.get(chatId);
                    if (id != null) {
                        repository.updateDescription(id, text);
                        sendText(chatId, "Описание обновлено ✅");
                    }
                    userStates.put(chatId, BotState.NONE);
                    return;
                }

                case WAITING_FOR_EQUIPMENT_FOR_LOC -> {
                    Equipment eq = findEquipmentByText(text);
                    if (eq == null) {
                        sendText(chatId, "Оборудование не найдено. Попробуйте ещё раз.");
                    } else {
                        tempEquipmentId.put(chatId, eq.getId());
                        sendText(chatId, "Введите новое расположение:");
                        userStates.put(chatId, BotState.WAITING_FOR_NEW_LOC);
                    }
                    return;
                }

                case WAITING_FOR_NEW_LOC -> {
                    Integer id = tempEquipmentId.get(chatId);
                    if (id != null) {
                        repository.updateLocation(id, text);
                        sendText(chatId, "Расположение обновлено ✅");
                    }
                    userStates.put(chatId, BotState.NONE);
                    return;
                }
            }


            // --- Проверка: если сообщение с фото ---
            if (update.getMessage().hasPhoto()) {
                try {
                    // Получаем файл от Telegram
                    String fileId = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId();
                    org.telegram.telegrambots.meta.api.objects.File file = execute(new GetFile(fileId));
                    java.io.File downloaded = downloadFile(file);

                    // Читаем QR
                    String qrText = qr.QrReader.readQr(downloaded.getAbsolutePath());

                    if (qrText != null) {


                        if (qrText.startsWith("ID:")) {
                            String idStr = qrText.split("\n")[0].replace("ID:", "").trim();
                            int id = Integer.parseInt(idStr);
                            sendEquipmentById(chatId, id);
                        }
                    } else {
                        sendText(chatId, "❌ Не удалось распознать QR");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendText(chatId, "⚠ Ошибка при обработке QR-кода");
                }
                return;
            }

            // --- Поиск по тексту (как раньше) ---
            String messageText = update.getMessage().getText().trim();
            if (messageText.equals("/start")) {
                sendKeyboard(chatId, "Привет! Я бот для поиска оборудования.\n" +
                        "Ты можешь искать оборудование по названию, серийному номеру или QR-коду.\n" +
                        "Также доступны кнопки для редактирования:");
                return;
            }


            List<Equipment> found = repository.searchEquipment(messageText);
            if (found.isEmpty()) {
                sendText(chatId, "❌ Оборудование не найдено по запросу: " + messageText);
            } else {
                StringBuilder response = new StringBuilder("✅ Найдено оборудование:\n\n");
                for (Equipment eq : found) {
                    response.append("ID: ").append(eq.getId()).append("\n")
                            .append("Название: ").append(eq.getName()).append("\n")
                            .append("Серийный номер: ").append(eq.getSerialNumber()).append("\n")
                            .append("Описание: ").append(eq.getDescription()).append("\n")
                            .append("Расположение: ").append(eq.getLocation()).append("\n")
                            .append("--------------------\n");
                }
                sendText(chatId, response.toString());
            }
        }
    }


    private void sendEquipmentById(Long chatId, int id) {
        Equipment eq = repository.getAllEquipment().stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);

        if (eq != null) {
            String msg = "📦 Оборудование #" + eq.getId() +
                    "\nНазвание: " + eq.getName() +
                    "\nСерийный: " + eq.getSerialNumber() +
                    "\nОписание: " + eq.getDescription() +
                    "\nРасположение: " + eq.getLocation();
            sendText(chatId, msg);
        } else {
            sendText(chatId, "❌ Не найдено оборудование с ID " + id);
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendKeyboard(Long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        // Создаем клавиатуру
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // кнопки подгоняются под экран
        keyboardMarkup.setOneTimeKeyboard(false); // клавиатура всегда остаётся

        // Ряды кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Изменить описание"));
        row1.add(new KeyboardButton("Изменить расположение"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Показать все"));

        keyboardMarkup.setKeyboard(List.of(row1, row2));

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private Equipment findEquipmentByText(String text) {
        // Пытаемся найти по ID
        try {
            int id = Integer.parseInt(text);
            return repository.getAllEquipment().stream()
                    .filter(e -> e.getId() == id)
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException ignored) {
            // Если не число — ищем по названию или серийному номеру
        }

        List<Equipment> found = repository.searchEquipment(text);
        if (!found.isEmpty()) {
            return found.get(0); // берём первый результат
        }
        return null;
    }


}
