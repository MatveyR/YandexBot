package ru.insomnic76.YandexBot.Bot.Services;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.insomnic76.YandexBot.Bot.Config.BotConfig;
import ru.insomnic76.YandexBot.Bot.Handlers.YandexBotUpdateHandler;

@Service
public class YandexBotService {

    private final TelegramBot bot;
    private final YandexBotUpdateHandler updateHandler;

    public YandexBotService(BotConfig botConfig) {
        this.bot = new TelegramBot(botConfig.telegramToken());
        updateHandler = new YandexBotUpdateHandler(bot, botConfig.targetChatId());
    }

    @PostConstruct
    public void init() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                updateHandler.handleUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}