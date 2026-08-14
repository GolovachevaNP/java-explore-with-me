package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewCommentDto {

    @NotNull
    private String text; // текст комментария, от 1 до 1000 символов
}