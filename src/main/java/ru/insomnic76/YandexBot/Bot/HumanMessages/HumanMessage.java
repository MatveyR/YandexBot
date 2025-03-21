package ru.insomnic76.YandexBot.Bot.HumanMessages;

import lombok.ToString;

@ToString
public enum HumanMessage {
    HELLO(new HumanMessageProps(
            "Привет! Я бот Yango Group. Если хочешь поделиться проектом просто нажми кнопку ниже.",
            "Hello! I'm a bot by Yango Group. If you want to share your project, simply click the button below."
    )),
    START_SHARING(new HumanMessageProps(
            "Поделиться проектом",
            "Share my project"
    )),
    INPUT_USER_NAME(new HumanMessageProps(
            "Введи свой ник в телеграмме через \"@\":",
            "Enter your nickname in the telegram via \"@\""
    )),
    INPUT_PROJECT_NAME(new HumanMessageProps(
            "Укажи название вашего проекта:",
            "Please enter the name of your project"
    )),
    INPUT_PROJECT_FROM(new HumanMessageProps(
            "Напиши одним сообщением откуда ваш проект: регион, страна и город:",
            "Write in one message where your project is from: region, country and city:"
    )),
    INPUT_PROJECT_DESCRIPTION(new HumanMessageProps(
            "Опиши проект: О чем он? Какие цели? Каких результатов добились? Также укажите команду проекта.",
            "Describe your project: what is it about? what are the goals? Also specify the project team."
    )),
    INPUT_PROJECT_MEDIA(new HumanMessageProps(
            "Прикрепи файлы проекта: фото, видео, документы. Когда загрузишь все файлы нажми кнопку \"/done\".",
            "Attach your project files: pictures, videos, documents. When all the files are uploaded, click \"/done\"."
    )),
    INPUT_PROJECT_COMPLETE(new HumanMessageProps(
            "Отлично! Твой проект загружается.." + System.lineSeparator() + "Пиши ещё, если захотите добавить новый проект.",
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
