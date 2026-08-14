package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicCommentDto {

    private Long id; // идентификатор комментария
    private Long authorId; // идентификатор автора
    private String authorName; // имя автора
    private String text; // текст комментария
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn; // дата и время создания комментария
    private Boolean edited; // признак того, что комментарий редактировался после первой публикации
}