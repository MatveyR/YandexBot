package ru.insomnic76.YandexBot.Bot.Config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "yandex", ignoreUnknownFields = false)
public record YandexDiskConfig(
        @NotEmpty String yandexOAuthToken,
        @NotEmpty String remotePath,
        @NotEmpty String localTempFilePath
) {
}