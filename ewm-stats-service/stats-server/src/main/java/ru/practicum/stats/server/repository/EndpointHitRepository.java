package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {
    List<EndpointHit> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<EndpointHit> findAllByTimestampBetweenAndUriIn(LocalDateTime start, LocalDateTime end, List<String> uris);
}