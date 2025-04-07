package ru.insomnic76.YandexBot.Bot.Handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import lombok.AllArgsConstructor;
import ru.insomnic76.YandexBot.Bot.HumanMessages.HumanMessage;
import ru.insomnic76.YandexBot.Bot.UserState.UserState;
import ru.insomnic76.YandexBot.Bot.UserState.UserStep;
import ru.insomnic76.YandexBot.Bot.Utilities.YandexDiskUtility;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
public class YandexBotUpdateHandler {
    private final TelegramBot bot;
    private final Map<Long, UserState> userStates = new HashMap<>();

    public void handleUpdate(Update update) {
        long chatId;
        String languageCode;
        UserStep step;

        if (update.message() != null) {
            chatId = update.message().from().id();
            languageCode = update.message().from().languageCode();
            step = UserStep.GREET;
        } else if (update.callbackQuery() != null) {
            chatId = update.callbackQuery().from().id();
            languageCode = update.callbackQuery().from().languageCode();
            if (Objects.equals(update.callbackQuery().data(), "add_one_more_project")) {
                step = UserStep.SHARE_MY_PROJECT;
            } else {
                step = UserStep.GREET;
            }
        } else {
            return;
        }

        if (!userStates.containsKey(chatId)) {
            userStates.put(chatId, new UserState(chatId, step, languageCode));
        }

        UserState state = userStates.get(chatId);

        switch (state.getStep()) {
            case GREET:
                handleGreet(update, state);
                break;
            case SHARE_MY_PROJECT:
                handleStartSharing(update, state);
                break;
            case PROJECT_NAME:
                handleGetProjectName(update, state);
                break;
            case PROJECT_FROM:
                handleGetProjectFrom(update, state);
                break;
            case PROJECT_DESCRIPTION:
                handleGetProjectDescription(update, state);
                break;
            case PROJECT_MEDIA:
                handleGetProjectMedia(update, state);
                break;
            case COMPLETE:
                break;
        }

        if (update.callbackQuery() != null) {
            bot.execute(new AnswerCallbackQuery(update.callbackQuery().id()));
        }
    }

    private void handleGreet(Update update, UserState state) {
        SendMessage sendMessage = new SendMessage(state.getChatId(), HumanMessage.HELLO.toString(state.getLanguageCode()));
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton(HumanMessage.START_SHARING.toString(state.getLanguageCode()))
                        .callbackData("start_sharing")
        );

        state.setStep(UserStep.SHARE_MY_PROJECT);
        bot.execute(sendMessage.replyMarkup(inlineKeyboard));
    }

    private void handleStartSharing(Update update, UserState state) {
        if (update.callbackQuery() == null) {
            return;
        }

        state.setStep(UserStep.PROJECT_NAME);
        state.setUserName("@" + update.callbackQuery().from().username());
        SendMessage sendMessage = new SendMessage(
                state.getChatId(),
                HumanMessage.INPUT_PROJECT_NAME.toString(state.getLanguageCode())
        );
        bot.execute(sendMessage);
    }

    private void handleGetProjectName(Update update, UserState state) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }
        SendMessage sendMessage = new SendMessage(
                state.getChatId(),
                HumanMessage.INPUT_PROJECT_FROM.toString(state.getLanguageCode())
        );

        state.setStep(UserStep.PROJECT_FROM);
        state.setProjectName(update.message().text().trim());

        initProject(state);

        bot.execute(sendMessage);
    }

    private void handleGetProjectFrom(Update update, UserState state) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }
        SendMessage sendMessage = new SendMessage(
                state.getChatId(),
                HumanMessage.INPUT_PROJECT_DESCRIPTION.toString(state.getLanguageCode())
        );

        state.setStep(UserStep.PROJECT_DESCRIPTION);
        state.setProjectFrom(update.message().text().trim());

        bot.execute(sendMessage);
    }

    private void handleGetProjectDescription(Update update, UserState state) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }
        SendMessage sendMessage = new SendMessage(
                state.getChatId(),
                HumanMessage.INPUT_PROJECT_MEDIA.toString(state.getLanguageCode())
        );
        ReplyKeyboardMarkup keyboardMarkup =
                new ReplyKeyboardMarkup("/done")
                        .oneTimeKeyboard(true)
                        .resizeKeyboard(true);

        state.setStep(UserStep.PROJECT_MEDIA);
        state.setProjectDesc(update.message().text().trim());

        YandexDiskUtility.createAndUploadProjectDetails(
                state.getUserName(),
                state.getProjectName(),
                state.getProjectFrom(),
                state.getProjectDesc()
        );
        bot.execute(sendMessage.replyMarkup(keyboardMarkup));
    }

    private void handleGetProjectMedia(Update update, UserState state) {
        if (update.message() == null) {
            return;
        }

        String fileId;
        String fileName;

        if (update.message().text() != null && update.message().text().startsWith("/done")) {
            SendMessage sendMessage = new SendMessage(
                    state.getChatId(),
                    HumanMessage.INPUT_PROJECT_COMPLETE.toString(state.getLanguageCode())
            );
            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton(HumanMessage.ONE_MORE_PROJECT_BUTTON.toString(state.getLanguageCode()))
                            .callbackData("add_one_more_project")
            );

            bot.execute(sendMessage.replyMarkup(inlineKeyboard));
            userStates.remove(state.getChatId());
        } else if (update.message().photo() != null) {
            PhotoSize[] photos = update.message().photo();
            if (photos.length < 1) {
                return;
            }
            PhotoSize photo = photos[photos.length - 1];
            fileId = photo.fileId();
            fileName = photo.fileUniqueId();
            uploadFile(fileId, state.getProjectName(), state.getUserName(), fileName, "photo");
        } else if (update.message().video() != null) {
            fileId = update.message().video().fileId();
            fileName = update.message().video().fileName();
            uploadFile(fileId, state.getProjectName(), state.getUserName(), fileName, "video");
        } else if (update.message().document() != null) {
            fileId = update.message().document().fileId();
            fileName = update.message().document().fileName();
            uploadFile(fileId, state.getProjectName(), state.getUserName(), fileName, "");
        } else if (update.message().audio() != null) {
            fileId = update.message().audio().fileId();
            fileName = update.message().audio().fileName();
            uploadFile(fileId, state.getProjectName(), state.getUserName(), fileName, "");
        }
    }

    private void uploadFile(String fileId, String projectName, String username, String filename, String fileType) {
        GetFile getFile = new GetFile(fileId);
        GetFileResponse getFileResponse = bot.execute(getFile);

        if (getFileResponse.isOk()) {
            File file = getFileResponse.file();
            String filePath = file.filePath();
            String fileUrl = "https://api.telegram.org/file/bot" + bot.getToken() + "/" + filePath;
            String fileFolder = projectName + "-" + username;
            if (Objects.equals(fileType, "photo")) {
                fileFolder += "/Картинки";
            } else if (Objects.equals(fileType, "video")) {
                fileFolder += "/Видео";
            } else {
                fileFolder += "/Прочие файлы";
            }

            try {
                YandexDiskUtility.uploadFileUniversal(fileUrl, fileFolder, filename);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("File response from TG is not OK");
        }
    }

    private void initProject(UserState state) {
        try {
            YandexDiskUtility.makeDir(state.getProjectName() + "-" + state.getUserName());
            YandexDiskUtility.makeDir(state.getProjectName() + "-" + state.getUserName() + "/Видео");
            YandexDiskUtility.makeDir(state.getProjectName() + "-" + state.getUserName() + "/Картинки");
            YandexDiskUtility.makeDir(state.getProjectName() + "-" + state.getUserName() + "/Прочие файлы");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
