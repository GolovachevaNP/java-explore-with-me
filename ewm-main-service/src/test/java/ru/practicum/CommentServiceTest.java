package ru.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.comment.dto.AdminCommentDto;
import ru.practicum.comment.dto.UserCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.service.CommentService;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    // Проверка создания комментария со статусом PENDING
    @Test
    void shouldCreateCommentWithPendingStatus() {
        User user = new User();
        user.setId(1L);

        Event event = new Event();
        event.setId(1L);
        event.setState(EventState.PUBLISHED);

        NewCommentDto request = new NewCommentDto();
        request.setText("Комментарий");

        UserCommentDto expected = new UserCommentDto();
        expected.setId(1L);
        expected.setText("Комментарий");
        expected.setStatus(CommentStatus.PENDING);
        expected.setEdited(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        when(commentRepository.save(ArgumentMatchers.any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment savedComment = invocation.getArgument(0);
                    savedComment.setId(1L);
                    return savedComment;
                });

        when(commentMapper.toUserCommentDto(ArgumentMatchers.any(Comment.class)))
                .thenReturn(expected);

        UserCommentDto actual = commentService.create(1L, 1L, request);

        assertEquals(expected, actual);
        verify(commentRepository).save(ArgumentMatchers.any(Comment.class));
    }

    // Проверка ошибки при создании комментария к неопубликованному событию
    @Test
    void shouldThrowExceptionWhenEventIsNotPublished() {
        User user = new User();
        user.setId(1L);

        Event event = new Event();
        event.setId(1L);
        event.setState(EventState.PENDING);

        NewCommentDto request = new NewCommentDto();
        request.setText("Комментарий");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class, () -> commentService.create(1L, 1L, request));
    }

    // Проверка повторной отправки опубликованного комментария на модерацию после изменения
    @Test
    void shouldSendPublishedCommentToModerationAfterUpdate() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("Старый комментарий");
        comment.setStatus(CommentStatus.PUBLISHED);
        comment.setEdited(false);

        NewCommentDto request = new NewCommentDto();
        request.setText("Новый комментарий");

        UserCommentDto expected = new UserCommentDto();
        expected.setId(1L);
        expected.setText("Новый комментарий");
        expected.setStatus(CommentStatus.PENDING);
        expected.setEdited(true);

        when(commentRepository.findByIdAndAuthorIdAndEventId(1L, 1L, 1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toUserCommentDto(comment)).thenReturn(expected);

        UserCommentDto actual = commentService.update(1L, 1L, 1L, request);

        assertEquals(expected, actual);
        assertEquals("Новый комментарий", comment.getText());
        assertEquals(CommentStatus.PENDING, comment.getStatus());
        assertTrue(comment.getEdited());
        verify(commentRepository).save(comment);
    }

    // Проверка публикации комментария администратором
    @Test
    void shouldPublishPendingComment() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setStatus(CommentStatus.PENDING);

        AdminCommentDto expected = new AdminCommentDto();
        expected.setId(1L);
        expected.setStatus(CommentStatus.PUBLISHED);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toAdminCommentDto(comment)).thenReturn(expected);

        AdminCommentDto actual = commentService.updateStatus(1L, CommentStatus.PUBLISHED);

        assertEquals(expected, actual);
        assertEquals(CommentStatus.PUBLISHED, comment.getStatus());
        verify(commentRepository).save(comment);
    }

    // Проверка удаления комментария его автором
    @Test
    void shouldDeleteCommentByAuthor() {
        Comment comment = new Comment();
        comment.setId(1L);

        when(commentRepository.findByIdAndAuthorIdAndEventId(1L, 1L, 1L)).thenReturn(Optional.of(comment));

        commentService.delete(1L, 1L, 1L);

        verify(commentRepository).delete(comment);
    }
}