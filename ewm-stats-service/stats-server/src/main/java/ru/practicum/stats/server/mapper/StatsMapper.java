package ru.practicum.stats.server.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatsMapper {
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
        EndpointHit firstHit = hits.get(0);
        long count = unique
                ? hits.stream().map(EndpointHit::getIp).distinct().count()
                : hits.size();

        return new ViewStatsDto(firstHit.getApp(), firstHit.getUri(), count);
    }
}