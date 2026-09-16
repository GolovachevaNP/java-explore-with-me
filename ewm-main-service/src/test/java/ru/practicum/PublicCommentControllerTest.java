package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.comment.controller.PublicCommentController;
import ru.practicum.comment.dto.PublicCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCommentController.class)
class PublicCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    // Проверка получения опубликованных комментариев
    @Test
    void shouldGetPublishedComments() throws Exception {
        PublicCommentDto comment = new PublicCommentDto();
        comment.setId(1L);
        comment.setAuthorId(2L);
        comment.setAuthorName("Имя");
        comment.setText("Комментарий");
        comment.setEdited(false);

        when(commentService.getPublishedComments(1L, 0, 10)).thenReturn(List.of(comment));

        mockMvc.perform(get("/events/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].text").value("Комментарий"))
                .andExpect(jsonPath("$[0].authorName").value("Имя"));

        verify(commentService).getPublishedComments(1L, 0, 10);
    }
}