package ru.practicum.event.model;

public enum EventState {
    PENDING,   // ожидает публикации
    PUBLISHED, // опубликовано
    CANCELED   // отменено
}