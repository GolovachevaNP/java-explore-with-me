package ru.practicum.stats.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleDateError() {
        return Map.of(
                "error", "Неверный формат даты",
                "message", "Ожидаемый формат даты: yyyy-MM-dd HH:mm:ss"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleArgumentError(IllegalArgumentException exception) {
        return Map.of(
                "error", "Неверные параметры запроса",
                "message", exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationError() {
        return Map.of(
                "error", "Некорректные данные",
                "message", "Поля: app, uri, ip и timestamp - обязательны"
        );
    }
}