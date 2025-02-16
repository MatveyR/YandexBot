package ru.insomnic76.YandexBot.Bot.UserState;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserState {
        long chatId;
        UserStep step;
        String languageCode;
        String userName = "";
        String projectName = "";
        String projectFrom = "";
        String projectDesc = "";

        public UserState(long chatId, UserStep step, String languageCode) {
                this.chatId = chatId;
                this.step = step;
                this.languageCode = languageCode;
        }
}
