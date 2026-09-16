package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.comment.controller.PrivateCommentController;
import ru.practicum.comment.dto.UserCommentDto;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.service.CommentService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateCommentController.class)
class PrivateCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    // Проверка создания комментария
    @Test
    void shouldCreateComment() throws Exception {
        UserCommentDto comment = new UserCommentDto();
        comment.setId(1L);
        comment.setText("Комментарий");
        comment.setStatus(CommentStatus.PENDING);

        when(commentService.create(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(comment);

        mockMvc.perform(post("/users/1/events/1/comments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Комментарий\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(commentService).create(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any()
        );
    }

    // Проверка получения пользователем списка своих комментариев
    @Test
    void shouldGetUserComments() throws Exception {
        UserCommentDto comment = new UserCommentDto();
        comment.setId(1L);
        comment.setEventId(2L);
        comment.setEventTitle("Тестовое событие");
        comment.setText("Комментарий");
        comment.setStatus(CommentStatus.PUBLISHED);
        comment.setEdited(false);

        when(commentService.getUserComments(1L, CommentStatus.PUBLISHED, 0, 10)).thenReturn(List.of(comment));

        mockMvc.perform(get("/users/1/comments").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventTitle").value("Тестовое событие"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(commentService).getUserComments(1L, CommentStatus.PUBLISHED, 0, 10);
    }
}