package ru.practicum.stats.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EndpointHitDto {
    private Long id; // идентификатор записи

    @NotBlank
    private String app; // идентификатор сервиса для которого записывается информация

    @NotBlank
    private String uri; // URI для которого был осуществлен запрос

    @NotBlank
    private String ip; // IP-адрес пользователя, осуществившего запрос

    @NotBlank
    private String timestamp; // дата и время, когда был совершен запрос к эндпоинту (в формате "yyyy-MM-dd HH:mm:ss")
}