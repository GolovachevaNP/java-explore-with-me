package ru.practicum.event.dto;

import lombok.Data;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.model.EventState;
import ru.practicum.user.dto.UserShortDto;

@Data
public class EventFullDto {

    private Long id; // идентификатор
    private String annotation; // краткое описание
    private CategoryDto category; // категория
    private Long confirmedRequests; // количество одобренных заявок на участие в данном событии
    private String createdOn; // дата и время создания события (в формате "yyyy-MM-dd HH:mm:ss")
    private String description; // полное описание события
    private String eventDate; // дата и время на которые намечено событие (в формате "yyyy-MM-dd HH:mm:ss")
    private UserShortDto initiator; // пользователь (краткая информация)
    private Location location; // широта и долгота места проведения события
    private Boolean paid; // нужно ли оплачивать участие
    private Integer participantLimit;  // ограничение на количество участников. Значение 0 - означает отсутствие ограничения
    private String publishedOn; // дата и время публикации события (в формате "yyyy-MM-dd HH:mm:ss")
    private Boolean requestModeration; // нужна ли пре-модерация заявок на участие
    private EventState state; // список состояний жизненного цикла события
    private String title; // заголовок
    private Long views; //количество просмотрев события
}