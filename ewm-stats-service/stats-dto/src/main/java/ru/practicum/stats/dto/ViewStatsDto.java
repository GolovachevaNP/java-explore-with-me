package ru.practicum.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatsDto {
    private String app; // название сервиса
    private String uri; // URI сервиса
    private Long hits;  // количество просмотров
}