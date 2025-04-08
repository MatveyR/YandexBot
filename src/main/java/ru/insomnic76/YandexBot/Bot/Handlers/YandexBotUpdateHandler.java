package ru.insomnic76.YandexBot.Bot.Handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.AllArgsConstructor;
import ru.insomnic76.YandexBot.Bot.HumanMessages.HumanMessage;
import ru.insomnic76.YandexBot.Bot.UserState.UserState;
import ru.insomnic76.YandexBot.Bot.UserState.UserStep;

import java.util.*;

@AllArgsConstructor
public class YandexBotUpdateHandler {
    private final TelegramBot bot;
    private final long targetChatId;
    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<Long, List<InputMedia<?>>> projectMedia = new HashMap<>();

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
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup("/done")
                .oneTimeKeyboard(true)
                .resizeKeyboard(true);

        state.setStep(UserStep.PROJECT_MEDIA);
        state.setProjectDesc(update.message().text().trim());
        bot.execute(sendMessage.replyMarkup(keyboardMarkup));
    }

    private void handleGetProjectMedia(Update update, UserState state) {
        if (update.message() == null) {
            return;
        }

        if (update.message().text() != null && update.message().text().startsWith("/done")) {
            sendProjectToTargetChat(state);

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
            projectMedia.remove(state.getChatId());
        } else {
            saveMediaForProject(update, state);
        }
    }

    private void saveMediaForProject(Update update, UserState state) {
        final long MAX_FILE_SIZE = 1536 * 1024 * 1024;
        List<InputMedia<?>> mediaList = projectMedia.computeIfAbsent(state.getChatId(), k -> new ArrayList<>());

        try {
            if (update.message().photo() != null) {
                PhotoSize[] photos = update.message().photo();
                if (photos.length > 0) {
                    PhotoSize photo = photos[photos.length - 1];
                    if (photo.fileSize() <= MAX_FILE_SIZE) {
                        mediaList.add(new InputMediaPhoto(photo.fileId()));
                    } else {
                        bot.execute(new SendMessage(state.getChatId(), HumanMessage.ERROR_LARGE_FILE.toString()));
                    }
                }
            }
            else if (update.message().video() != null) {
                Video video = update.message().video();
                if (video.fileSize() <= MAX_FILE_SIZE) {
                    mediaList.add(new InputMediaVideo(video.fileId()));
                } else {
                    bot.execute(new SendMessage(state.getChatId(), HumanMessage.ERROR_LARGE_FILE.toString()));
                }
            }
            else if (update.message().document() != null) {
                Document document = update.message().document();
                if (document.fileSize() <= MAX_FILE_SIZE) {
                    mediaList.add(new InputMediaDocument(document.fileId()));
                } else {
                    bot.execute(new SendMessage(state.getChatId(), HumanMessage.ERROR_LARGE_FILE.toString()));
                }
            }
            else if (update.message().audio() != null) {
                mediaList.add(new InputMediaAudio(update.message().audio().fileId()));
            }
        } catch (Exception e) {
            bot.execute(new SendMessage(state.getChatId(),
                    "⚠️ Ошибка при обработке файла. Попробуйте отправить его снова."));
        }
    }

    private void sendProjectToTargetChat(UserState state) {
        String projectTag = "#" + state.getProjectName().replaceAll("\\s+", "_").toLowerCase();
        String fullDescription = state.getProjectDesc();

        String header = String.format(
                """
                📌 *Новый проект!* %s #проект
                
                👤 Автор: %s
                🏷 Название: %s
                📍 Откуда: %s
                """,
                projectTag,
                state.getUserName(),
                state.getProjectName(),
                state.getProjectFrom()
        );

        int availableCaptionLength = 1024 - header.length() - 10;
        boolean isDescriptionTooLong = fullDescription.length() > availableCaptionLength;

        String caption = header + "📝 Описание:\n" +
                (isDescriptionTooLong
                        ? fullDescription.substring(0, availableCaptionLength - 4) + "..."
                        : fullDescription);

        List<InputMedia<?>> mediaList = projectMedia.get(state.getChatId());

        if (mediaList == null || mediaList.isEmpty()) {
            sendTextMessageWithSplitting(targetChatId,
                    header + "📝 Полное описание:\n" + fullDescription,
                    projectTag);
            return;
        }

        List<InputMedia<?>> documents = new ArrayList<>();
        List<InputMedia<?>> otherFiles = new ArrayList<>();

        for (InputMedia<?> media : mediaList) {
            if (media instanceof InputMediaDocument) {
                documents.add(media);
            } else if ((media instanceof InputMediaVideo) || (media instanceof InputMediaPhoto)) {
                otherFiles.add(media);
            }
        }

        if (otherFiles.isEmpty() && !documents.isEmpty()) {
            bot.execute(new SendMessage(targetChatId, caption).parseMode(ParseMode.Markdown));

            if (isDescriptionTooLong) {
                String remainingText = fullDescription.substring(availableCaptionLength - 4);
                sendTextMessageWithSplitting(targetChatId,
                        "📝 Описание (продолжение):\n" + remainingText,
                        projectTag);
            }
        }

        if (!otherFiles.isEmpty()) {
            InputMedia<?>[] mainBatch = otherFiles.stream()
                    .limit(10)
                    .toArray(InputMedia<?>[]::new);

            if (mainBatch.length > 0 && (mainBatch[0] instanceof InputMediaPhoto)) {
                ((InputMediaPhoto) mainBatch[0]).caption(caption).parseMode(ParseMode.Markdown);
            } else if (mainBatch.length > 0 && (mainBatch[0] instanceof InputMediaVideo)) {
                ((InputMediaVideo) mainBatch[0]).caption(caption).parseMode(ParseMode.Markdown);
            }
            bot.execute(new SendMediaGroup(targetChatId, mainBatch));

            if (isDescriptionTooLong) {
                String remainingText = fullDescription.substring(availableCaptionLength - 4);
                sendTextMessageWithSplitting(targetChatId,
                        "📝 Описание (продолжение):\n" + remainingText,
                        projectTag);
            }

            if (otherFiles.size() > 10) {
                String continuationCaption = projectTag + " #проект #продолжение";
                for (int i = 10; i < otherFiles.size(); i += 10) {
                    InputMedia<?>[] batch = otherFiles.stream()
                            .skip(i)
                            .limit(10)
                            .toArray(InputMedia<?>[]::new);

                    if (batch.length > 0 && batch[0] instanceof InputMediaPhoto) {
                        ((InputMediaPhoto) batch[0]).caption(continuationCaption).parseMode(ParseMode.Markdown);
                    } else if (batch.length > 0 && batch[0] instanceof InputMediaVideo) {
                        ((InputMediaVideo) batch[0]).caption(continuationCaption).parseMode(ParseMode.Markdown);
                    }
                    bot.execute(new SendMediaGroup(targetChatId, batch));
                }
            }
        }

        if (!documents.isEmpty()) {
            String docsCaption = "📎 Документы проекта:\n" + projectTag + " #проект #документы";

            InputMedia<?>[] docsBatch = documents.stream()
                    .limit(10)
                    .toArray(InputMedia<?>[]::new);

            if (docsBatch.length > 0) {
                ((InputMediaDocument) docsBatch[docsBatch.length - 1]).caption(docsCaption).parseMode(ParseMode.Markdown);
            }
            bot.execute(new SendMediaGroup(targetChatId, docsBatch));

            if (documents.size() > 10) {
                String docsContinuation = projectTag + " #проект #документы #продолжение";
                for (int i = 10; i < documents.size(); i += 10) {
                    InputMedia<?>[] batch = documents.stream()
                            .skip(i)
                            .limit(10)
                            .toArray(InputMedia<?>[]::new);

                    if (batch.length > 0) {
                        ((InputMediaDocument) batch[batch.length - 1]).caption(docsContinuation).parseMode(ParseMode.Markdown);
                    }
                    bot.execute(new SendMediaGroup(targetChatId, batch));
                }
            }
        }
    }

    private void sendTextMessageWithSplitting(Long chatId, String text, String projectTag) {
        int maxLength = 4096 - projectTag.length() - 10;
        String hashtags = "\n\n" + projectTag + " #проект";

        for (int i = 0; i < text.length(); i += maxLength) {
            String part = text.substring(i, Math.min(i + maxLength, text.length()));
            boolean isLastPart = (i + maxLength) >= text.length();

            String message = isLastPart ? part + hashtags : part;
            bot.execute(new SendMessage(chatId, message).parseMode(ParseMode.Markdown));
        }
    }
}