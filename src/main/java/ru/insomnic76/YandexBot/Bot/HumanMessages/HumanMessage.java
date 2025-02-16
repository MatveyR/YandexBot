package ru.insomnic76.YandexBot.Bot.HumanMessages;

import lombok.ToString;

@ToString
public enum HumanMessage {
    HELLO(new HumanMessageProps(
            "Привет! Я бот Яндекса. Если хочешь поделиться проектом просто нажми кнопку ниже.",
            "Hello! I`m a bot by Yandex. If you want to share your project just click button below"
    )),
    START_SHARING(new HumanMessageProps(
            "Поделиться проектом",
            "Share my project"
    )),
    INPUT_USER_NAME(new HumanMessageProps(
            "Введите ваш ник в телеграмме через \"@\":",
            "Enter your nickname in the telegram via \"@\""
    )),
    INPUT_PROJECT_NAME(new HumanMessageProps(
            "Укажи название вашего проекта:",
            "Input yours project name:"
    )),
    INPUT_PROJECT_FROM(new HumanMessageProps(
            "Напиши одним сообщением откуда ваш проект: регион, страна и город:",
            "Write in one message where your project is from: region, country and city:"
    )),
    INPUT_PROJECT_DESCRIPTION(new HumanMessageProps(
            "Опишите ваш проект: о чем он? какие цели? Также укажите команду проекта.",
            "Describe your project: what is it about? what are the goals? Also specify the project team."
    )),
    INPUT_PROJECT_MEDIA(new HumanMessageProps(
            "Прикрепите файлы вашего проекта: картинки, видео, документы. Когда загрузите все файлы нажмите кнопку \"/done\".",
            "Attach your project files: pictures, videos, documents. When all the files are uploaded, click \"/done\"."
    )),
    INPUT_PROJECT_COMPLETE(new HumanMessageProps(
            "Отлично! Ваш проект загружается..." + System.lineSeparator() + "Пишите ещё, если захотите добавить новый проект.",
            "Great! Your project is being uploaded..." + System.lineSeparator() + "Write more if you want to add a new project."
    ));

    private final HumanMessageProps humanMessageProps;

    HumanMessage(HumanMessageProps humanMessageProps) {
        this.humanMessageProps = humanMessageProps;
    }

    public String toString(String code) {
        switch (code) {
            case "ru":
                return humanMessageProps.rusMessage();
            default:
                return humanMessageProps.engMessage();
        }
    }
}
