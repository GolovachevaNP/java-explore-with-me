package ru.practicum.stats.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.EndpointHitRepository;
import ru.practicum.stats.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private EndpointHitRepository repository;

    @Mock
    private StatsMapper statsMapper;

    @InjectMocks
    private StatsService statsService;

    // Проверка преобразования DTO в сущность и сохранения обращения
    @Test
    void shouldSaveHitAsEndpointHit() {
        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp("ewm-main-service");
        dto.setUri("/events/1");
        dto.setIp("192.168.1.1");
        dto.setTimestamp("2026-11-15 10:00:00");
        EndpointHit mappedHit = new EndpointHit();
        when(statsMapper.toEndpointHit(dto)).thenReturn(mappedHit);

        statsService.saveHit(dto);

        verify(repository).save(mappedHit);
        assertEquals(LocalDateTime.of(2026, 11, 15, 10, 0), mappedHit.getTimestamp());
        verify(statsMapper).toEndpointHit(dto);
    }

    // Проверка получения статистики без фильтра URI
    @Test
    void getStatsWithoutUrisShouldUseAllStatsQuery() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 15, 11, 0);
        EndpointHit firstHit = hit("ewm-main-service", "/events/1", "192.168.1.1");
        EndpointHit secondHit = hit("ewm-main-service", "/events/1", "192.168.1.2");
        when(repository.findAllByTimestampBetween(start, end))
                .thenReturn(List.of(firstHit, secondHit));
        when(statsMapper.toViewStatsDtoList(List.of(firstHit, secondHit), false))
                .thenReturn(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 2L)));

        List<ViewStatsDto> actual = statsService.getStats(start, end, null, false);

        assertEquals(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 2L)), actual);
        verify(repository).findAllByTimestampBetween(start, end);
        verify(statsMapper).toViewStatsDtoList(List.of(firstHit, secondHit), false);
    }

    // Проверка получения статистики с фильтром по URI
    @Test
    void getStatsWithUrisShouldUseUriFilterQuery() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 15, 11, 0);
        List<String> uris = List.of("/events/1");
        EndpointHit firstHit = hit("ewm-main-service", "/events/1", "192.168.1.1");
        EndpointHit secondHit = hit("ewm-main-service", "/events/1", "192.168.1.1");
        when(repository.findAllByTimestampBetweenAndUriIn(start, end, uris))
                .thenReturn(List.of(firstHit, secondHit));
        when(statsMapper.toViewStatsDtoList(List.of(firstHit, secondHit), true))
                .thenReturn(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 1L)));

        List<ViewStatsDto> actual = statsService.getStats(start, end, uris, true);

        assertEquals(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 1L)), actual);
        verify(repository).findAllByTimestampBetweenAndUriIn(start, end, uris);
        verify(statsMapper).toViewStatsDtoList(List.of(firstHit, secondHit), true);
    }

    // Проверка отклонения периода, в котором дата начала позже даты окончания
    @Test
    void getStatsShouldRejectReversedPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 15, 11, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 15, 9, 0);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> statsService.getStats(start, end, null, false)
        );
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