package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final Exception exception) {
        log.warn("Ошибка валидации: {}", exception.getMessage());

        return createError(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                "Переданы некорректные данные"
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(final NotFoundException exception) {
        log.warn("Объект не найден: {}", exception.getMessage());

        return createError(
                HttpStatus.NOT_FOUND,
                "Запрашиваемый объект не найден",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(final ConflictException exception) {
        log.warn("Конфликт данных: {}", exception.getMessage());

        return createError(
                HttpStatus.CONFLICT,
                "Условия для выполнения операции не выполнены",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(final IllegalArgumentException exception) {
        log.warn("Некорректный запрос: {}", exception.getMessage());

        return createError(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                exception.getMessage()
        );
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable exception) {
        log.error("Внутренняя ошибка сервера", exception);

        return createError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера.",
                "Произошла непредвиденная ошибка"
        );
    }

    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleDateError(DateTimeParseException exception) {
        log.warn("Неверный формат даты: {}", exception.getParsedString());

        return createError(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                "Ожидаемый формат даты: yyyy-MM-dd HH:mm:ss"
        );
    }

    private ApiError createError(HttpStatus status, String reason, String message) {
        List<String> errors = new ArrayList<>();
        errors.add(message);

        return new ApiError(errors, message, reason, status.toString(), LocalDateTime.now().format(FORMATTER));
    }
}