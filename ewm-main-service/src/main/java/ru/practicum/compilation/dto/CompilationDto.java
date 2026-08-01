package ru.practicum.compilation.dto;

import lombok.Data;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

@Data
public class CompilationDto {
    private Long id; // идентификатор
    private List<EventShortDto> events; // список событий входящих в подборку
    private boolean pinned; // закреплена ли подборка на главной странице сайта
    private String title; // заголовок подборки
}