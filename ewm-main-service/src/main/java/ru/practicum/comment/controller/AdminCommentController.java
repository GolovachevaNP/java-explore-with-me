package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.AdminCommentDto;
import ru.practicum.comment.dto.UpdateCommentStatusRequest;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<AdminCommentDto> getComments(
            @RequestParam(defaultValue = "PENDING") CommentStatus status,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size
    ) {
        log.info("Администратор запрашивает комментарии со статусом status={}", status);

        return commentService.getAdminComments(status, from, size);
    }

    @PatchMapping("/{commentId}")
    public AdminCommentDto updateCommentStatus(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentStatusRequest dto
    ) {
        log.info("Администратор изменяет статус комментария с id={} на {}", commentId, dto.getStatus());

        return commentService.updateStatus(commentId, dto.getStatus());
    }
}