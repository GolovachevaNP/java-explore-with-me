package ru.practicum.stats.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleDateError(DateTimeParseException exception) {
        log.warn("Неверный формат даты: {}", exception.getParsedString());

        return Map.of(
                "error", "Неверный формат даты",
                "message", "Ожидаемый формат даты: yyyy-MM-dd HH:mm:ss"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleArgumentError(IllegalArgumentException exception) {
        log.warn("Неверные параметры запроса: {}", exception.getMessage());

        return Map.of(
                "error", "Неверные параметры запроса",
                "message", exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationError(MethodArgumentNotValidException exception) {
        log.warn("Некорректные данные обращения: {}", exception.getMessage());

        return Map.of(
                "error", "Некорректные данные",
                "message", "Поля: app, uri, ip и timestamp - обязательны"
        );
    }
}