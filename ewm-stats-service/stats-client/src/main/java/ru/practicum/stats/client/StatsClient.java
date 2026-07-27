package ru.practicum.stats.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class StatsClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String serverUrl;

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void hit(EndpointHitDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/hit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            send(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Не удалось преобразовать данные в JSON", e);
        }
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        String url = serverUrl + "/stats"
                + "?start=" + encode(start)
                + "&end=" + encode(end);

        if (uris != null) {
            for (String uri : uris) {
                url += "&uris=" + encode(uri);
            }
        }

        url += "&unique=" + unique;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            checkStatus(response);
            ViewStatsDto[] stats = objectMapper.readValue(
                    response.body(),
                    ViewStatsDto[].class
            );
            return Arrays.asList(stats);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Запрос к серверу статистики прерван", e);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось получить или обработать ответ сервера статистики", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            checkStatus(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Запрос к серверу статистики прерван", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Не удалось отправить запрос к серверу статистики", e);
        }
    }

    private void checkStatus(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Сервер статистики вернул ошибку: " + response.statusCode()
            );
        }
    }
}