package ru.practicum.request.dto;

import lombok.Data;
import ru.practicum.request.model.RequestStatus;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds; // идентификаторы запросов на участие в событии текущего пользователя
    private RequestStatus status;  // новый статус запроса на участие в событии текущего пользователя
}