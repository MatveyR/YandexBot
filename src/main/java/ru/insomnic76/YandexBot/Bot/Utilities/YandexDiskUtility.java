package ru.insomnic76.YandexBot.Bot.Utilities;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.insomnic76.YandexBot.Bot.Config.YandexDiskConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
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

    public YandexDiskUtility(YandexDiskConfig config) {
        OAUTH_TOKEN = config.yandexOAuthToken();
        REMOTE_PATH = config.remotePath();
        LOCAL_TEMP_FILE_PATH = config.localTempFilePath();

        new File(LOCAL_TEMP_FILE_PATH).mkdirs();
    }

    public static void uploadFileUniversal(String fileUrl, String remotePath, String filename) throws IOException, ProtocolException {
        long fileSize = getFileSize(fileUrl);

        System.out.println(fileUrl);
        System.out.println(remotePath);
        System.out.println(filename);

        if (fileSize <= 50 * 1024 * 1024) {
            uploadViaUrl(fileUrl, remotePath, filename);
        } else if (fileSize <= 100 * 1024 * 1024) {
            String tempFilePath = downloadFileToTemp(fileUrl, filename);
            uploadSimple(tempFilePath, remotePath + "/" + filename);
            Files.deleteIfExists(Paths.get(tempFilePath));
        } else {
            System.out.println("File is too large.");
            String tempFilePath = downloadFileToTemp(fileUrl, filename);
            uploadLargeFile(tempFilePath, remotePath + "/" + filename);
            Files.deleteIfExists(Paths.get(tempFilePath));
        }
    }

    public static void makeDir(String folderName) throws IOException {
        String requestUrl = MAKE_DIR_URL + "?path=" + URLEncoder.encode(REMOTE_PATH + folderName, StandardCharsets.UTF_8);
        HttpPut httpPut = new HttpPut(requestUrl);
        httpPut.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPut)) {
            if (response.getCode() != 201) {
                throw new IOException("Ошибка создания папки. Код: " + response.getCode());
            }
        }
    }

    public static void createAndUploadProjectDetails(String userName, String projectName,
                                                     String projectFrom, String projectDesc) {
        String tempFilePath = LOCAL_TEMP_FILE_PATH + "project_info_" + System.currentTimeMillis() + ".txt";
        String text = "Ник: " + userName + System.lineSeparator()
                + "Проект: " + projectName + System.lineSeparator()
                + "Локация: " + projectFrom + System.lineSeparator()
                + "\nОписание:\n" + projectDesc;

        try {
            Files.writeString(Paths.get(tempFilePath), text, StandardCharsets.UTF_8);
            uploadSimple(tempFilePath, REMOTE_PATH + projectName + "-" + userName + "/Описание.txt");
            Files.deleteIfExists(Paths.get(tempFilePath));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static long getFileSize(String fileUrl) throws IOException, ProtocolException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpHead httpHead = new HttpHead(fileUrl);
            try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
                return Long.parseLong(response.getHeader("Content-Length").getValue());
            }
        }
    }

    private static String downloadFileToTemp(String fileUrl, String filename) throws IOException {
        String tempPath = LOCAL_TEMP_FILE_PATH + filename;
        try (InputStream in = new URL(fileUrl).openStream();
             OutputStream out = Files.newOutputStream(Paths.get(tempPath))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempPath;
    }

    private static void uploadViaUrl(String fileUrl, String remotePath, String filename) throws IOException {
        String requestUrl = UPLOAD_URL + "?path=" + URLEncoder.encode(REMOTE_PATH + remotePath + "/" + filename, StandardCharsets.UTF_8)
                + "&url=" + URLEncoder.encode(fileUrl, StandardCharsets.UTF_8);

        HttpPost httpPost = new HttpPost(requestUrl);
        httpPost.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getCode() != 202) {
                throw new IOException("Ошибка прямой загрузки. Код: " + response.getCode());
            }
        }
    }

    private static void uploadSimple(String localPath, String remotePath) throws IOException {
        String uploadUrl = getUploadLink(remotePath);
        HttpPut httpPut = new HttpPut(uploadUrl);
        httpPut.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

        httpPut.setEntity(new InputStreamEntity(
                Files.newInputStream(Paths.get(localPath)),
                Files.size(Paths.get(localPath)),
                ContentType.DEFAULT_BINARY
        ));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPut)) {
            if (response.getCode() != 201) {
                throw new IOException("Ошибка загрузки. Код: " + response.getCode());
            }
        }
    }

    private static void uploadLargeFile(String localPath, String remotePath) throws IOException {
        String uploadUrl = getUploadLink(remotePath);
        try (InputStream in = Files.newInputStream(Paths.get(localPath))) {
            byte[] buffer = new byte[10 * 1024 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                uploadChunk(uploadUrl, buffer, bytesRead);
            }
        }
    }

    private static void uploadChunk(String uploadUrl, byte[] chunk, int length) throws IOException {
        HttpPut httpPut = new HttpPut(uploadUrl);
        httpPut.setHeader("Content-Type", "application/octet-stream");

        httpPut.setEntity(new ByteArrayEntity(chunk, 0, length, ContentType.APPLICATION_OCTET_STREAM));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPut)) {
            if (response.getCode() != 200 && response.getCode() != 201) {
                throw new IOException("Ошибка загрузки чанка. Код: " + response.getCode());
            }
        }
    }

    private static String getUploadLink(String remotePath) throws IOException {
        String url = UPLOAD_URL + "?path=" + URLEncoder.encode(remotePath, StandardCharsets.UTF_8);
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "OAuth " + OAUTH_TOKEN);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {

            String json = EntityUtils.toString(response.getEntity());
            JSONObject jsonResponse = new JSONObject(json);

            if (jsonResponse.has("href")) {
                return jsonResponse.getString("href");
            } else if (jsonResponse.has("error")) {
                throw new IOException("Yandex.Disk error: " + jsonResponse.getString("error"));
            } else {
                throw new IOException("Unexpected response from Yandex.Disk: " + json);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}