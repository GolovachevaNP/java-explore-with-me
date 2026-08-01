package ru.practicum.stats.server.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "endpoint_hits")
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // идентификатор записи
    private String app; // идентификатор сервиса для которого записывается информация
    private String uri; // URI для которого был осуществлен запрос
    private String ip;  // IP-адрес пользователя, осуществившего запрос
    private LocalDateTime timestamp; // дата и время, когда был совершен запрос к эндпоинту (в формате "yyyy-MM-dd HH:mm:ss")
}