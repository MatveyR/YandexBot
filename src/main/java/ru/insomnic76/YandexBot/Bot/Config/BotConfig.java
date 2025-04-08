package ru.insomnic76.YandexBot.Bot.Config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bot", ignoreUnknownFields = false)
public record BotConfig(@NotEmpty String telegramToken, Long targetChatId) {}
