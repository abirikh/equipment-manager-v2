package bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class BotMain {
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("config.properties"));

        String token = props.getProperty("BOT_TOKEN");
        String username = props.getProperty("BOT_USERNAME");

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new EquipmentBot());
            System.out.println("✅ Бот запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

