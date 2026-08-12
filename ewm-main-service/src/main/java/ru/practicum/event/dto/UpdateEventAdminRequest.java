package ru.practicum.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.event.model.AdminStateAction;

@Data
public class UpdateEventAdminRequest {

    @Size(min = 20, max = 2000)
    private String annotation; // новая аннотация
    private Long category; // новая категория
    @Size(min = 20, max = 7000)
    private String description; // новое описание
    private String eventDate; // новые дата и время на которые намечено событие. Дата и время указываются в формате "yyyy-MM-dd HH:mm:ss"
    @Valid
    private Location location; // широта и долгота места проведения события
    private Boolean paid; // новое значение флага о платности мероприятия
    @PositiveOrZero
    private Integer participantLimit; // новый лимит пользователей
    private Boolean requestModeration; // нужна ли пре-модерация заявок на участие
    private AdminStateAction stateAction; // новое состояние события
    @Size(min = 3, max = 120)
    private String title; // новый заголовок
}