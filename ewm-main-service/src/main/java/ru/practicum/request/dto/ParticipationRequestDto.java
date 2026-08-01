package ru.practicum.request.dto;

import lombok.Data;
import ru.practicum.request.model.RequestStatus;

@Data
public class ParticipationRequestDto {

    private Long id; // идентификатор заявки
    private Long event; // идентификатор события
    private String created; // дата и время создания заявки
    private Long requester; // идентификатор пользователя, отправившего заявку
    private RequestStatus status; // статус заявки
}