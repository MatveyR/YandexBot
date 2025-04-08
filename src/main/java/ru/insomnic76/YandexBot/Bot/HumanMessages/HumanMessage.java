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
            "Опиши проект: О чем он? Какие цели? Каких результатов добились? Также укажи команду проекта" + System.lineSeparator() + "Максимальный размер описания - 4000 символов",
            "Describe your project: what is it about? what are the goals? Also specify the project team" + System.lineSeparator() + "The maximum description size is 4000 characters"
    )),
    INPUT_PROJECT_MEDIA(new HumanMessageProps(
            "Прикрепи файлы проекта: фото, видео, документы " + System.lineSeparator() + "Размер каждого отдельного файла ограничен 1.5 гигабайтами" + System.lineSeparator() + "Когда загрузишь все файлы нажми кнопку \"/done\"",
            "Attach your project files: pictures, videos, documents " + System.lineSeparator() + "The size of each file is limited to 1.5 gigabytes" + System.lineSeparator() + "When all the files are uploaded, click \"/done\""
    )),
    ERROR_LARGE_FILE(new HumanMessageProps(
            "Размер файла превышает 1.5 гигабайта! Файл не сохранён",
            "The file size exceeds 1.5 GB! The file was not saved"
    )),
    INPUT_PROJECT_COMPLETE(new HumanMessageProps(
            "Отлично! Твой проект загружается.." + System.lineSeparator() + "Пиши ещё, если захочешь добавить новый проект.",
            "Great! Your project is being uploaded..." + System.lineSeparator() + "Write more if you want to add a new project."
    )),
    ONE_MORE_PROJECT_BUTTON(new HumanMessageProps(
            "Добавить ещё проект",
            "Add one more project"
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
