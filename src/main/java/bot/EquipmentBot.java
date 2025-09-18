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
import repository.EquipmentRepository;
import repository.SQLiteEquipmentRepository;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class EquipmentBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final EquipmentRepository repository;

    // --- Состояния для пошагового редактирования ---
    private enum BotState {
        NONE,
        WAITING_FOR_EQUIPMENT_FOR_DESC,
        WAITING_FOR_NEW_DESC,
        WAITING_FOR_EQUIPMENT_FOR_LOC,
        WAITING_FOR_NEW_LOC
    }

    private final Map<Long, BotState> userStates = new HashMap<>();
    private final Map<Long, Integer> tempEquipmentId = new HashMap<>();

    public EquipmentBot() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить config.properties", e);
        }

        this.botToken = props.getProperty("BOT_TOKEN");
        this.botUsername = props.getProperty("BOT_USERNAME");
        this.repository = new SQLiteEquipmentRepository();
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
        if (!update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();

        // --- Если фото (QR-код) ---
        if (update.getMessage().hasPhoto()) {
            handleQrInput(chatId, update);
            return;
        }

        // --- Если текст ---
        String text = update.getMessage().getText().trim();
        if (text.equals("/start")) {
            sendKeyboard(chatId, "Привет! Я бот для работы с оборудованием.\n" +
                    "Можешь искать оборудование по названию, серийному номеру, ответственному, расположению или QR-коду.\n" +
                    "Также доступны кнопки для редактирования.");
            return;
        }

        // --- Обработка кнопок ---
        if (text.equals("Изменить ответственного")) {
            sendText(chatId, "Введите ID, название или отправьте QR-код оборудования:");
            userStates.put(chatId, BotState.WAITING_FOR_EQUIPMENT_FOR_DESC);
            return;
        }

        if (text.equals("Изменить расположение")) {
            sendText(chatId, "Введите ID, название или отправьте QR-код оборудования:");
            userStates.put(chatId, BotState.WAITING_FOR_EQUIPMENT_FOR_LOC);
            return;
        }

        if (text.equals("Показать все")) {
            List<Equipment> all = repository.getAllEquipment();
            if (all.isEmpty()) {
                sendText(chatId, "📭 В базе пока нет оборудования");
            } else {
                StringBuilder sb = new StringBuilder("📋 Все оборудование:\n\n");
                for (Equipment eq : all) {
                    sb.append(formatEquipment(eq)).append("\n--------------------\n");
                }
                sendText(chatId, sb.toString());
            }
            return;
        }

        // --- Проверка состояния пользователя ---
        BotState state = userStates.getOrDefault(chatId, BotState.NONE);

        switch (state) {
            case WAITING_FOR_EQUIPMENT_FOR_DESC -> {
                Equipment eq = findEquipmentByText(text);
                if (eq == null) {
                    sendText(chatId, "❌ Оборудование не найдено. Попробуйте ещё раз или отправьте QR.");
                } else {
                    tempEquipmentId.put(chatId, eq.getId());
                    sendText(chatId, "Введите нового ответственного:");
                    userStates.put(chatId, BotState.WAITING_FOR_NEW_DESC);
                }
                return;
            }

            case WAITING_FOR_NEW_DESC -> {
                Integer id = tempEquipmentId.get(chatId);
                if (id != null) {
                    repository.updateDescription(id, text);
                    sendText(chatId, "✅ Ответственный обновлён");
                }
                userStates.put(chatId, BotState.NONE);
                return;
            }

            case WAITING_FOR_EQUIPMENT_FOR_LOC -> {
                Equipment eq = findEquipmentByText(text);
                if (eq == null) {
                    sendText(chatId, "❌ Оборудование не найдено. Попробуйте ещё раз или отправьте QR.");
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
                    sendText(chatId, "✅ Расположение обновлено");
                }
                userStates.put(chatId, BotState.NONE);
                return;
            }
        }

        // --- Поиск по тексту ---
        List<Equipment> found = repository.searchEquipment(text);
        if (found.isEmpty()) {
            sendText(chatId, "❌ Не найдено: " + text);
        } else {
            StringBuilder sb = new StringBuilder("✅ Найдено оборудование:\n\n");
            for (Equipment eq : found) {
                sb.append(formatEquipment(eq)).append("\n--------------------\n");
            }
            sendText(chatId, sb.toString());
        }
    }

    // --- Обработка QR ---
    private void handleQrInput(Long chatId, Update update) {
        try {
            String fileId = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId();
            org.telegram.telegrambots.meta.api.objects.File file = execute(new GetFile(fileId));
            java.io.File downloaded = downloadFile(file);

            String qrText = qr.QrReader.readQr(downloaded.getAbsolutePath());
            if (qrText == null) {
                sendText(chatId, "❌ Не удалось распознать QR");
                return;
            }

            if (qrText.startsWith("ID:")) {
                int id = Integer.parseInt(qrText.split("\n")[0].replace("ID:", "").trim());
                Equipment eq = repository.getAllEquipment().stream()
                        .filter(e -> e.getId() == id)
                        .findFirst()
                        .orElse(null);

                BotState state = userStates.getOrDefault(chatId, BotState.NONE);
                if (state == BotState.WAITING_FOR_EQUIPMENT_FOR_DESC) {
                    if (eq != null) {
                        tempEquipmentId.put(chatId, eq.getId());
                        sendText(chatId, "Введите нового ответственного:");
                        userStates.put(chatId, BotState.WAITING_FOR_NEW_DESC);
                    } else {
                        sendText(chatId, "❌ Не найдено оборудование с таким QR");
                    }
                } else if (state == BotState.WAITING_FOR_EQUIPMENT_FOR_LOC) {
                    if (eq != null) {
                        tempEquipmentId.put(chatId, eq.getId());
                        sendText(chatId, "Введите новое расположение:");
                        userStates.put(chatId, BotState.WAITING_FOR_NEW_LOC);
                    } else {
                        sendText(chatId, "❌ Не найдено оборудование с таким QR");
                    }
                } else {
                    if (eq != null) {
                        sendText(chatId, "📦 Оборудование по QR:\n" + formatEquipment(eq));
                    } else {
                        sendText(chatId, "❌ Не найдено оборудование по QR");
                    }
                }
            } else {
                sendText(chatId, "⚠ QR не содержит ID оборудования");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "⚠ Ошибка при обработке QR");
        }
    }

    // --- Вспомогательные методы ---
    private String formatEquipment(Equipment eq) {
        return "ID: " + eq.getId() +
                "\nНазвание: " + eq.getName() +
                "\nСерийный: " + eq.getSerialNumber() +
                "\nОтветственный: " + eq.getDescription() +
                "\nРасположение: " + eq.getLocation();
    }

    private Equipment findEquipmentByText(String text) {
        try {
            int id = Integer.parseInt(text);
            return repository.getAllEquipment().stream()
                    .filter(e -> e.getId() == id)
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException ignored) {
        }

        List<Equipment> found = repository.searchEquipment(text);
        return found.isEmpty() ? null : found.get(0);
    }

    private void sendText(Long chatId, String text) {
        try {
            execute(new SendMessage(String.valueOf(chatId), text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendKeyboard(Long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Изменить ответственного"));
        row1.add(new KeyboardButton("Изменить расположение"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Показать все"));

        keyboard.setKeyboard(List.of(row1, row2));
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
