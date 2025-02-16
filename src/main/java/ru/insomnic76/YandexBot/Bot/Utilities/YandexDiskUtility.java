package ru.insomnic76.YandexBot.Bot.Utilities;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.insomnic76.YandexBot.Bot.Config.YandexDiskConfig;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class YandexDiskUtility {

    private static final String UPLOAD_URL = "https://cloud-api.yandex.net/v1/disk/resources/upload";
    private static final String MAKE_DIR_URL = "https://cloud-api.yandex.net/v1/disk/resources";

    private static String OAUTH_TOKEN;
    private static String REMOTE_PATH;
    private static String LOCAL_TEMP_FILE_PATH;

    private YandexDiskUtility(YandexDiskConfig config) {
        OAUTH_TOKEN = config.yandexOAuthToken();
        REMOTE_PATH = config.remotePath();
        LOCAL_TEMP_FILE_PATH = config.localTempFilePath();
    }

    public static void uploadFileToDisk(String fileUrl, String projectName, String filename) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String requestUrl =
                    UPLOAD_URL
                            + "?path="
                            + URLEncoder.encode(REMOTE_PATH + projectName + "/" + filename, StandardCharsets.UTF_8)
                            + "&url="
                            + URLEncoder.encode(fileUrl, StandardCharsets.UTF_8);
            HttpPost httpPost = new HttpPost(requestUrl);
            httpPost.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                if (statusCode == 202) {
                    System.out.println("Медиа-файл загружен!");
                } else {
                    System.err.println("Яндекс отклонил запрос. Код ошибки: " + statusCode);
                }
            } catch (IOException e) {
                System.err.println("Ошибка при отправке запроса: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Ошибка при создании http-клиента: " + e.getMessage());
        }
    }

    public static void makeDir(String folderName) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String requestUrl =
                    MAKE_DIR_URL
                            + "?path="
                            + URLEncoder.encode(REMOTE_PATH + folderName, StandardCharsets.UTF_8);
            HttpPut httpPut = new HttpPut(requestUrl);
            httpPut.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                int statusCode = response.getCode();
                if (statusCode == 201) {
                    System.out.println("Папка успешно создана!");
                } else {
                    System.err.println("Яндекс отклонил запрос запрос создания папки. Код ошибки: " + statusCode);
                }
            } catch (IOException e) {
                System.err.println("Ошибка при отправке запроса создания папки: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Ошибка при создании http-клиента: " + e.getMessage());
        }
    }

    public static void createAndUploadProjectDetailsToDisk(String userName, String projectName, String projectFrom, String projectDescription) {
        java.io.File file = null;
        String filePath = LOCAL_TEMP_FILE_PATH + userName;
        try {
            file = new java.io.File(filePath);
            if (file.createNewFile()) {
                System.out.println("Файл создан");

                try {
                    String text =
                            "Ник отправителя: " + userName + System.lineSeparator()
                                    + "Название проекта: " + projectName + System.lineSeparator()
                                    + "Локация: " + projectFrom + System.lineSeparator()
                                    + "Описание проекта: " + System.lineSeparator() + projectDescription + System.lineSeparator();
                    Files.writeString(Paths.get(filePath), text, StandardCharsets.UTF_32);
                } catch (Exception e) {
                    System.err.println("Не удалось записать в созданный файл: " + e.getMessage());
                }
            } else {
                System.out.println("Не удалось создать файл");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        uploadLocalFileToDisk(projectName + "-" + userName + "/Описание.txt", filePath);

        try {
            assert file != null;
            if (file.delete()) {
                System.out.println("Временный файл удалён");
            } else {
                System.out.println("Ошибка при удалении временного файла");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при удалении временного файла2");
        }
    }

    public static void uploadLocalFileToDisk(String projectName, String filePath) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String uploadUrl = getLocalFileUploadLink(REMOTE_PATH + projectName);

            if (uploadUrl != null) {
                HttpPost httpPost = new HttpPost(uploadUrl);
                HttpEntity entity = MultipartEntityBuilder.create()
                        .addBinaryBody("file", new File(filePath), ContentType.DEFAULT_BINARY, "file")
                        .build();
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getCode();
                    if (statusCode == 201) {
                        System.out.println("Файл успешно загружен на Яндекс.Диск: " + projectName);
                    } else {
                        System.err.println("Ошибка при загрузке файла. Код ответа: " + statusCode);
                    }
                }
            } else {
                System.err.println("Не удалось получить ссылку для загрузки.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке файла: " + e.getMessage());
        }
    }

    public static String getLocalFileUploadLink(String yandexPath) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String requestUrl = UPLOAD_URL + "?path=" + URLEncoder.encode(yandexPath) + "&overwrite=true";
            HttpGet httpGet = new HttpGet(requestUrl);
            httpGet.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    String responseBody = EntityUtils.toString(entity);
                    return new JSONObject(responseBody).getString("href");
                } else {
                    System.err.println("Ошибка при получении ссылки для загрузки. Код ответа: " + statusCode);
                    return null;
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при получении ссылки для загрузки: " + e.getMessage());
            return null;
        } catch (ParseException e) {
            System.err.println("Ошибка парсинга");
            return null;
        }
    }
}