package ru.practicum.stats.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.stats.dto.EndpointHitDto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsClientTest {
    private HttpServer server;
    private StatsClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        client = new StatsClient("http://localhost:" + server.getAddress().getPort());
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // Проверка отправки обращения в сервис статистики
    @Test
    void shouldSendHit() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/hit", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            send(exchange, 201, "");
        });

        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp("ewm-main-service");
        dto.setUri("/events/1");
        dto.setIp("192.168.1.1");
        dto.setTimestamp("2026-11-15 10:00:00");

        client.hit(dto);

        assertTrue(requestBody.get().contains("\"app\":\"ewm-main-service\""));
        assertTrue(requestBody.get().contains("\"uri\":\"/events/1\""));
    }

    // Проверка запроса статистики с несколькими URI
    @Test
    void shouldGetStats() {
        server.createContext("/stats", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("start=2026-11-15+09%3A00%3A00&end=2026-11-15+11%3A00%3A00&uris=%2Fevents%2F1&uris=%2Fevents%2F2&unique=true",
                    exchange.getRequestURI().getRawQuery());
            send(exchange, 200, "[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":2},{\"app\":\"ewm-main-service\",\"uri\":\"/events/2\",\"hits\":1}]");
        });

        var result = client.getStats("2026-11-15 09:00:00", "2026-11-15 11:00:00",
                List.of("/events/1", "/events/2"), true);

        assertEquals(2, result.size());
        assertEquals("ewm-main-service", result.getFirst().getApp());
        assertEquals("/events/1", result.getFirst().getUri());
        assertEquals(2L, result.getFirst().getHits());
    }

    // Проверка запроса статистики без фильтра по URI
    @Test
    void shouldGetStatsWithoutUriFilter() {
        server.createContext("/stats", exchange -> {
            assertEquals("start=2026-11-15+09%3A00%3A00&end=2026-11-15+11%3A00%3A00&unique=false",
                    exchange.getRequestURI().getRawQuery());
            send(exchange, 200, "[]");
        });

        var result = client.getStats("2026-11-15 09:00:00", "2026-11-15 11:00:00", null, false);

        assertEquals(List.of(), result);
    }

    // Проверка обработки клиентом ответа сервера с ошибкой
    @Test
    void shouldThrowWhenServerReturnsError() {
        server.createContext("/hit", exchange -> send(exchange, 400, "bad request"));

        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp("ewm-main-service");
        dto.setUri("/events/1");
        dto.setIp("192.168.1.1");
        dto.setTimestamp("2026-11-15 10:00:00");

        assertThrows(IllegalStateException.class, () -> client.hit(dto));
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}