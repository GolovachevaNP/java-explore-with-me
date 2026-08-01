package ru.practicum.event.model;

public enum StateAction {
    SEND_TO_REVIEW, // отправить событие на модерацию
    CANCEL_REVIEW   // отменить отправку события на модерацию
}