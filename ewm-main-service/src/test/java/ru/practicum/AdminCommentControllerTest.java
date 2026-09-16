package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.comment.controller.AdminCommentController;
import ru.practicum.comment.dto.AdminCommentDto;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.service.CommentService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCommentController.class)
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    // Проверка получения ожидающих модерации комментариев администратором
    @Test
    void shouldGetPendingComments() throws Exception {
        AdminCommentDto comment = new AdminCommentDto();
        comment.setId(1L);
        comment.setEventId(2L);
        comment.setEventTitle("Концерт");
        comment.setAuthorId(3L);
        comment.setText("Комментарий");
        comment.setStatus(CommentStatus.PENDING);
        comment.setEdited(false);

        when(commentService.getAdminComments(CommentStatus.PENDING, 0, 10)).thenReturn(List.of(comment));

        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventTitle").value("Концерт"))
                .andExpect(jsonPath("$[0].authorId").value(3))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(commentService).getAdminComments(CommentStatus.PENDING, 0, 10);
    }

    // Проверка фильтра и пагинации комментариев администратора
    @Test
    void shouldGetCommentsWithStatusAndPagination() throws Exception {
        when(commentService.getAdminComments(CommentStatus.REJECTED, 5, 3)).thenReturn(List.of());

        mockMvc.perform(get("/admin/comments")
                        .param("status", "REJECTED")
                        .param("from", "5")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(commentService).getAdminComments(CommentStatus.REJECTED, 5, 3);
    }

    // Проверка публикации комментария администратором
    @Test
    void shouldPublishComment() throws Exception {
        AdminCommentDto comment = new AdminCommentDto();
        comment.setId(1L);
        comment.setText("Комментарий");
        comment.setStatus(CommentStatus.PUBLISHED);

        when(commentService.updateStatus(1L, CommentStatus.PUBLISHED)).thenReturn(comment);

        mockMvc.perform(patch("/admin/comments/1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(commentService).updateStatus(1L, CommentStatus.PUBLISHED);
    }
}