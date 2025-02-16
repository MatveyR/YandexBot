package ru.insomnic76.YandexBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.insomnic76.YandexBot.Bot.Config.BotConfig;
import ru.insomnic76.YandexBot.Bot.Config.YandexDiskConfig;

@SpringBootApplication
@EnableConfigurationProperties({BotConfig.class, YandexDiskConfig.class})
public class YandexBotApplication {

    public static void main(String[] args) {
        System.out.println("Start!!");
        SpringApplication.run(YandexBotApplication.class, args);
    }

}
