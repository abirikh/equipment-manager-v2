package bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class BotMain {
    public static void main(String[] args) {
        String token = "8458380504:AAFTHb47N4DdyGtaSblyzGwaJgr_xBobiaU";
        String username = "RusenegroEquipmentBot";

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new EquipmentBot(token, username));
            System.out.println("✅ Бот запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
