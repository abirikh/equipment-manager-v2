package bot;

import db.Database;
import model.Equipment;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.startsWith("/id")) {
                try {
                    int id = Integer.parseInt(text.split(" ")[1]);
                    Equipment eq = database.getAllEquipment().stream()
                            .filter(e -> e.getId() == id)
                            .findFirst()
                            .orElse(null);

                    if (eq != null) {
                        String msg = "Оборудование #" + eq.getId() +
                                "\nНазвание: " + eq.getName() +
                                "\nСерийный: " + eq.getSerialNumber() +
                                "\nОписание: " + eq.getDescription() +
                                "\nРасположение: " + eq.getLocation();

                        send(chatId, msg);
                    } else {
                        send(chatId, "❌ Не найдено оборудование с ID " + id);
                    }
                } catch (Exception ex) {
                    send(chatId, "Ошибка: укажи ID, например `/id 1`");
                }
            } else {
                send(chatId, "Привет! Напиши `/id <номер>` чтобы получить данные.");
            }
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
