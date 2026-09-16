package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UserCommentDto;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCommentDto createComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto dto
    ) {
        log.info("Пользователь с id={} добавляет комментарий к событию с id={}", userId, eventId);

        return commentService.create(userId, eventId, dto);
    }

    @PatchMapping("/events/{eventId}/comments/{commentId}")
    public UserCommentDto updateComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @PathVariable Long commentId,
            @Valid @RequestBody NewCommentDto dto
    ) {
        log.info("Пользователь с id={} изменяет комментарий с id={}", userId, commentId);

        return commentService.update(userId, eventId, commentId, dto);
    }

    @DeleteMapping("/events/{eventId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @PathVariable Long commentId
    ) {
        log.info("Пользователь с id={} удаляет комментарий с id={}", userId, commentId);

        commentService.delete(userId, eventId, commentId);
    }

    @GetMapping("/comments")
    public List<UserCommentDto> getUserComments(
            @PathVariable Long userId,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size
    ) {
        log.info("Пользователь с id={} запрашивает свои комментарии со статусом status={}", userId, status);

        return commentService.getUserComments(userId, status, from, size);
    }
}