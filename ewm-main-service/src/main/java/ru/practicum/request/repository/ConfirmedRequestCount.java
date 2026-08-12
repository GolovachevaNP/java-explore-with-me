package ru.practicum.request.repository;

public interface ConfirmedRequestCount {
    Long getEventId();

    Long getConfirmedRequests();
}