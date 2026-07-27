package ru.practicum.stats.server;

import org.junit.jupiter.api.Test;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsMapperTest {
    private final StatsMapper mapper = new StatsMapper();

    // Проверка группировки обращений по приложению и URI
    @Test
    void shouldGroupHitsByApplicationAndUri() {
        List<EndpointHit> hits = List.of(
                hit("ewm", "/events/1", "192.168.1.1"),
                hit("ewm", "/events/1", "192.168.1.2"),
                hit("ewm", "/events/2", "192.168.1.1"),
                hit("other", "/events/1", "192.168.1.1")
        );

        List<ViewStatsDto> result = mapper.toViewStatsDtoList(hits, false);

        assertEquals(List.of(
                new ViewStatsDto("ewm", "/events/1", 2L),
                new ViewStatsDto("ewm", "/events/2", 1L),
                new ViewStatsDto("other", "/events/1", 1L)
        ), result);
    }

    // Проверка подсчёта уникальных IP-адресов для одного URI
    @Test
    void shouldCountUniqueIps() {
        List<EndpointHit> hits = List.of(
                hit("ewm", "/events/1", "192.168.1.1"),
                hit("ewm", "/events/1", "192.168.1.1"),
                hit("ewm", "/events/1", "192.168.1.2")
        );

        List<ViewStatsDto> result = mapper.toViewStatsDtoList(hits, true);

        assertEquals(List.of(
                new ViewStatsDto("ewm", "/events/1", 2L)
        ), result);
    }

    // Проверка отдельного подсчёта уникальных IP-адресов для каждого URI
    @Test
    void shouldCountUniqueIpsSeparatelyForEachUri() {
        List<EndpointHit> hits = List.of(
                hit("ewm", "/events/1", "192.168.1.1"),
                hit("ewm", "/events/1", "192.168.1.1"),
                hit("ewm", "/events/2", "192.168.1.1")
        );

        List<ViewStatsDto> result = mapper.toViewStatsDtoList(hits, true);

        assertEquals(List.of(
                new ViewStatsDto("ewm", "/events/1", 1L),
                new ViewStatsDto("ewm", "/events/2", 1L)
        ), result);
    }

    // Проверка сортировки статистики по убыванию количества просмотров
    @Test
    void shouldSortStatsByHitsInDescendingOrder() {
        List<EndpointHit> hits = List.of(
                hit("ewm", "/events/1", "192.168.1.1"),
                hit("ewm", "/events/2", "192.168.1.1"),
                hit("ewm", "/events/2", "192.168.1.2"),
                hit("ewm", "/events/2", "192.168.1.3")
        );

        List<ViewStatsDto> result = mapper.toViewStatsDtoList(hits, false);

        assertEquals(List.of(
                new ViewStatsDto("ewm", "/events/2", 3L),
                new ViewStatsDto("ewm", "/events/1", 1L)
        ), result);
    }

    // Проверка пустого результата при отсутствии обращений
    @Test
    void shouldReturnEmptyListForEmptyInput() {
        assertEquals(List.of(), mapper.toViewStatsDtoList(List.of(), false));
    }

    private EndpointHit hit(String app, String uri, String ip) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(app);
        hit.setUri(uri);
        hit.setIp(ip);
        hit.setTimestamp(LocalDateTime.of(2026, 11, 15, 10, 0));
        return hit;
    }
}