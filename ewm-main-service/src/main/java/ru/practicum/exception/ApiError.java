package ru.practicum.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ApiError {
    private List<String> errors; // список стектрейсов или описания ошибок
    private String message;   // сообщение об ошибке
    private String reason;    // общее описание причины ошибки
    private String status;    // код статуса HTTP-ответа
    private String timestamp; // дата и время когда произошла ошибка (в формате "yyyy-MM-dd HH:mm:ss")
}