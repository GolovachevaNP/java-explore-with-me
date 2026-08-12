package ru.practicum.user.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;      // идентификатор пользователя
    private String name;  // имя пользователя
    private String email; // почтовый адрес пользователя
}