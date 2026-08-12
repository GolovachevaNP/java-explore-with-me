package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EndpointHitRepository repository;
    private final StatsMapper statsMapper;

    @Transactional
    public void saveHit(EndpointHitDto dto) {
        EndpointHit hit = statsMapper.toEndpointHit(dto);
        hit.setTimestamp(LocalDateTime.parse(dto.getTimestamp(), FORMATTER));

        repository.save(hit);
        log.debug("Обращение сохранено: app={}, uri={}", hit.getApp(), hit.getUri());
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<EndpointHit> hits = uris == null || uris.isEmpty()
                ? repository.findAllByTimestampBetween(start, end)
                : repository.findAllByTimestampBetweenAndUriIn(start, end, uris);

        List<ViewStatsDto> stats = statsMapper.toViewStatsDtoList(hits, unique);
        log.debug("Статистика сформирована: количество записей={}", stats.size());

        return stats;
    }
}