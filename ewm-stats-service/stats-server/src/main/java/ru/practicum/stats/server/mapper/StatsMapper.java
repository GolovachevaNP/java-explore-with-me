package ru.practicum.stats.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class StatsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    public abstract EndpointHit toEndpointHit(EndpointHitDto dto);

    @Mapping(target = "app", source = "hit.app")
    @Mapping(target = "uri", source = "hit.uri")
    @Mapping(target = "hits", source = "hits")
    public abstract ViewStatsDto toViewStatsDto(EndpointHit hit, Long hits);

    public List<ViewStatsDto> toViewStatsDtoList(List<EndpointHit> hits, boolean unique) {
        Map<String, List<EndpointHit>> groupedHits = new LinkedHashMap<>();

        for (EndpointHit hit : hits) {
            String key = hit.getApp() + "," + hit.getUri();

            if (!groupedHits.containsKey(key)) {
                groupedHits.put(key, new ArrayList<>());
            }

            groupedHits.get(key).add(hit);
        }

        List<ViewStatsDto> result = new ArrayList<>();

        for (List<EndpointHit> group : groupedHits.values()) {
            ViewStatsDto dto = toViewStatsDto(group, unique);
            result.add(dto);
        }

        return result.stream()
                .sorted(Comparator.comparing(ViewStatsDto::getHits).reversed())
                .toList();
    }

    private ViewStatsDto toViewStatsDto(List<EndpointHit> hits, boolean unique) {
        if (hits.isEmpty()) {
            throw new IllegalArgumentException("Список посещений не должен быть пустым");
        }

        EndpointHit firstHit = hits.getFirst();
        long count = unique
                ? hits.stream().map(EndpointHit::getIp).distinct().count()
                : hits.size();

        return toViewStatsDto(firstHit, count);
    }
}