package ru.practicum.request.model;

public enum RequestStatus {
    PENDING,   // ожидает публикации
    CONFIRMED, // подтверждено
    REJECTED,  // отклонено
    CANCELED   // отменено
}