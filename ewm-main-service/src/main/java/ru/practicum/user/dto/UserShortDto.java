package ru.practicum.user.dto;

import lombok.Data;

@Data
public class UserShortDto {
    private Long id;     // идентификатор пользователя
    private String name; // имя пользователя
}