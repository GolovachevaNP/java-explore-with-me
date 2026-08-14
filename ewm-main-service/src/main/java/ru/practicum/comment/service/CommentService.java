package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.AdminCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.PublicCommentDto;
import ru.practicum.comment.dto.UserCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public UserCommentDto create(Long userId, Long eventId, NewCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя оставить комментарий к неопубликованному событию");
        }

        String text = getValidText(dto.getText());

        Comment comment = new Comment();
        comment.setText(text);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        comment.setEdited(false);
        comment.setEvent(event);
        comment.setAuthor(author);

        Comment savedComment = commentRepository.save(comment);

        log.info("Пользователь с id={} добавил комментарий с id={} к событию с id={}", userId, savedComment.getId(), eventId);

        return commentMapper.toUserCommentDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<PublicCommentDto> getPublishedComments(Long eventId, int from, int size) {
        eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с id=" + eventId + " не найдено"));

        if (from > Integer.MAX_VALUE - size) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, from + size);

        List<Comment> comments = commentRepository
                .findByEventIdAndStatusOrderByCreatedOnDesc(eventId, CommentStatus.PUBLISHED, pageable);

        return commentMapper.toPublicCommentDtoList(getCommentsPart(comments, from, size));
    }

    @Transactional
    public AdminCommentDto updateStatus(Long commentId, CommentStatus status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        if (status != CommentStatus.PUBLISHED && status != CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Статус комментария может быть только PUBLISHED или REJECTED");
        }

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно модерировать только комментарий в статусе PENDING");
        }

        comment.setStatus(status);
        Comment savedComment = commentRepository.save(comment);

        log.info("Администратор изменил статус комментария с id={} на {}", commentId, status);

        return commentMapper.toAdminCommentDto(savedComment);
    }

    @Transactional
    public UserCommentDto update(Long userId, Long eventId, Long commentId, NewCommentDto dto) {
        Comment comment = commentRepository.findByIdAndAuthorIdAndEventId(commentId, userId, eventId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        String text = getValidText(dto.getText());

        if (comment.getStatus() == CommentStatus.PUBLISHED) {
            comment.setEdited(true);
        }

        comment.setText(text);
        comment.setStatus(CommentStatus.PENDING);

        Comment savedComment = commentRepository.save(comment);

        log.info("Пользователь с id={} изменил комментарий с id={}", userId, commentId);

        return commentMapper.toUserCommentDto(savedComment);
    }

    @Transactional
    public void delete(Long userId, Long eventId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorIdAndEventId(commentId, userId, eventId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        commentRepository.delete(comment);

        log.info("Пользователь с id={} удалил комментарий с id={}", userId, commentId);
    }

    @Transactional(readOnly = true)
    public List<UserCommentDto> getUserComments(Long userId, CommentStatus status, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        if (from > Integer.MAX_VALUE - size) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, from + size);
        List<Comment> comments;

        if (status == null) {
            comments = commentRepository.findByAuthorIdOrderByCreatedOnDesc(userId, pageable);
        } else {
            comments = commentRepository.findByAuthorIdAndStatusOrderByCreatedOnDesc(userId, status, pageable);
        }

        return commentMapper.toUserCommentDtoList(getCommentsPart(comments, from, size));
    }

    @Transactional(readOnly = true)
    public List<AdminCommentDto> getAdminComments(CommentStatus status, int from, int size) {
        if (from > Integer.MAX_VALUE - size) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, from + size);
        List<Comment> comments;

        if (status == null) {
            comments = commentRepository.findAllByOrderByCreatedOnDesc(pageable);
        } else {
            comments = commentRepository.findByStatusOrderByCreatedOnDesc(status, pageable);
        }

        return commentMapper.toAdminCommentDtoList(getCommentsPart(comments, from, size));
    }

    private List<Comment> getCommentsPart(List<Comment> comments, int from, int size) {
        if (from >= comments.size()) {
            return List.of();
        }

        int toIndex = Math.min(from + size, comments.size());

        return comments.subList(from, toIndex);
    }

    private String getValidText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Текст комментария должен содержать от 1 до 1000 символов");
        }

        String validText = text.trim();

        if (validText.isEmpty() || validText.length() > 1000) {
            throw new IllegalArgumentException("Текст комментария должен содержать от 1 до 1000 символов");
        }

        return validText;
    }
}