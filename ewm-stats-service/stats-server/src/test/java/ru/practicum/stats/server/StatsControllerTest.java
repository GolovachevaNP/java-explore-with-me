package ru.practicum.stats.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EndpointHitRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    // Проверка сохранения обращения и получения статистики по нему
    @Test
    void shouldSaveHitAndReturnStats() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.1.1");

        mockMvc.perform(get("/stats")
                        .param("start", "2026-11-15 09:00:00")
                        .param("end", "2026-11-15 11:00:00")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(1));
    }

    // Проверка подсчёта только уникальных IP-адресов
    @Test
    void shouldCountUniqueIps() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.1.1");
        saveHit("ewm-main-service", "/events/1", "192.168.1.1");
        saveHit("ewm-main-service", "/events/1", "192.168.1.2");

        mockMvc.perform(get("/stats")
                        .param("start", "2026-11-15 09:00:00")
                        .param("end", "2026-11-15 11:00:00")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    // Проверка фильтрации статистики по одному URI
    @Test
    void shouldFilterStatsByUris() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.1.1");
        saveHit("ewm-main-service", "/events/2", "192.168.1.2");

        mockMvc.perform(get("/stats")
                        .param("start", "2026-11-15 09:00:00")
                        .param("end", "2026-11-15 11:00:00")
                        .param("uris", "/events/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uri").value("/events/2"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    // Проверка фильтрации статистики по нескольким URI
    @Test
    void shouldFilterStatsBySeveralUris() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.1.1");
        saveHit("ewm-main-service", "/events/2", "192.168.1.2");
        saveHit("ewm-main-service", "/events/3", "192.168.1.3");

        mockMvc.perform(get("/stats")
                        .param("start", "2026-11-15 09:00:00")
                        .param("end", "2026-11-15 11:00:00")
                        .param("uris", "/events/1", "/events/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[1].uri").value("/events/3"))
                .andExpect(jsonPath("$[2]").doesNotExist());
    }

    // Проверка включения обращений, сделанных ровно в начале и конце периода (граничные значения)
    @Test
    void shouldIncludeHitsAtPeriodBoundaries() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.1.1", "2026-11-15 09:00:00");
        saveHit("ewm-main-service", "/events/1", "192.168.1.2", "2026-11-15 11:00:00");

        mockMvc.perform(get("/stats")
                        .param("start", "2026-11-15 09:00:00")
                        .param("end", "2026-11-15 11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    // Проверка отклонения запроса с незаполненными обязательными полями
    @Test
    void shouldRejectInvalidHit() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"app\":\"ewm-main-service\"}"))
                .andExpect(status().isBadRequest());
    }

    // Проверка отклонения запроса с датой в неверном формате
    @Test
    void shouldRejectHitWithInvalidTimestamp() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\","
                                + "\"ip\":\"192.168.1.1\",\"timestamp\":\"15-11-2026 10:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    // Проверка отклонения периода, в котором начало позже конца
    @Test
    void shouldRejectReversedDateRange() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2026-11-15 11:00:00")
                        .param("end", "2026-11-15 09:00:00"))
                .andExpect(status().isBadRequest());
    }

    private void saveHit(String app, String uri, String ip) throws Exception {
        saveHit(app, uri, ip, "2026-11-15 10:00:00");
    }

    private void saveHit(String app, String uri, String ip, String timestamp) throws Exception {
        String hitJson = (
                "{\"app\":\"%s\",\"uri\":\"%s\",\"ip\":\"%s\","
                        + "\"timestamp\":\"%s\"}"
        ).formatted(app, uri, ip, timestamp);

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hitJson))
                .andExpect(status().isCreated());
    }
}