package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.practicum.comment.model.CommentStatus;

import java.time.LocalDateTime;

@Data
public class UserCommentDto {

    private Long id; // идентификатор комментария
    private Long eventId; // идентификатор события, к которому относится комменатрий
    private String eventTitle; // заголовок события
    private String text; // текст комментария, от 1 до 1000 символов
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn; // дата и время создания комментария
    private CommentStatus status; // статус модерации
    private Boolean edited; // признак того, что комментарий редактировался после первой публикации
}