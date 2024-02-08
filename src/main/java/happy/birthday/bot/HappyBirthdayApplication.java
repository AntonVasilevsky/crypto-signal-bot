package happy.birthday.bot;

import happy.birthday.bot.service.EventListener;
import happy.birthday.bot.service.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@Slf4j
public class HappyBirthdayApplication {
    private static EventListener eventListener;
    private static TelegramBot telegramBot;

    public HappyBirthdayApplication(EventListener eventListener, TelegramBot telegramBot) {
        HappyBirthdayApplication.eventListener = eventListener;
        HappyBirthdayApplication.telegramBot = telegramBot;
    }


    public static void main(String[] args) {
        SpringApplication.run(HappyBirthdayApplication.class, args);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> telegramBot.monitorSignalMatchFlag());
        executorService.submit(() -> {
            try {
                    while (true) {
                        eventListener.monitorApiData();
                        Thread.sleep(500);
                    }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        executorService.shutdown();
       // telegramBot.sendMessage(1724892054, "test message");


    }

}

