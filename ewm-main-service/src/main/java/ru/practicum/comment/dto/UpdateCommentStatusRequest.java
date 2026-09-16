package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.comment.model.CommentStatus;

@Data
public class UpdateCommentStatusRequest {

    @NotNull
    private CommentStatus status; // статус модерации
}