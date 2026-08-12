package ru.practicum.event.dto;

import lombok.Data;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;

@Data
public class EventShortDto {

    private String annotation; // краткое описание
    private CategoryDto category; // категория
    private Long confirmedRequests; // количество одобренных заявок на участие в данном событии
    private String eventDate; // дата и время на которые намечено событие (в формате "yyyy-MM-dd HH:mm:ss")
    private Long id; // идентификатор
    private UserShortDto initiator; // пользователь (краткая информация)
    private Boolean paid; // нужно ли оплачивать участие
    private String title; // заголовок
    private Long views; // количество просмотрев события
}