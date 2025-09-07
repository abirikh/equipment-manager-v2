package bot;

import db.Database;
import model.Equipment;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import qr.QrReader;

import java.io.InputStream;

public class EquipmentBot extends TelegramLongPollingBot {
    private final Database database = new Database();
    private final String token;
    private final String username;

    public EquipmentBot(String token, String username) {
        this.token = token;
        this.username = username;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();

            // 📌 Если текст
            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();

                if (text.startsWith("/id")) {
                    handleIdCommand(chatId, text);
                } else {
                    send(chatId, "Привет! Отправь мне `/id 1` или фото QR-кода с оборудованием.");
                }
            }

            // 📌 Если фото
            if (update.getMessage().hasPhoto()) {
                try {
                    var photo = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1);
                    var fileInfo = execute(new GetFile(photo.getFileId()));

                    try (InputStream input = downloadFileAsStream(fileInfo.getFilePath())) {
                        String qrText = QrReader.readQr(input);

                        if (qrText != null) {


                            if (qrText.startsWith("ID:")) {
                                String idStr = qrText.split("\n")[0].replace("ID:", "").trim();
                                int id = Integer.parseInt(idStr);
                                sendEquipmentById(chatId, id);
                            }
                        } else {
                            send(chatId, "❌ Не удалось распознать QR");
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    send(chatId, "⚠ Ошибка при обработке фото");
                }
            }
        }
    }

    private void handleIdCommand(Long chatId, String text) {
        try {
            int id = Integer.parseInt(text.split(" ")[1]);
            sendEquipmentById(chatId, id);
        } catch (Exception ex) {
            send(chatId, "⚠ Использование: /id <номер>");
        }
    }

    private void sendEquipmentById(Long chatId, int id) {
        Equipment eq = database.getAllEquipment().stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);

        if (eq != null) {
            String msg = "📦 Оборудование #" + eq.getId() +
                    "\nНазвание: " + eq.getName() +
                    "\nСерийный: " + eq.getSerialNumber() +
                    "\nОписание: " + eq.getDescription() +
                    "\nРасположение: " + eq.getLocation();
            send(chatId, msg);
        } else {
            send(chatId, "❌ Не найдено оборудование с ID " + id);
        }
    }

    private void send(Long chatId, String text) {
        try {
            execute(new SendMessage(String.valueOf(chatId), text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
