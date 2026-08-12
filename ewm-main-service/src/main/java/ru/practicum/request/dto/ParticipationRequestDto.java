package ru.practicum.request.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.practicum.request.model.RequestStatus;

import java.time.LocalDateTime;

@Data
public class ParticipationRequestDto {

    private Long id; // идентификатор заявки
    private Long event; // идентификатор события
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created; // дата и время создания заявки
    private Long requester; // идентификатор пользователя, отправившего заявку
    private RequestStatus status; // статус заявки
}