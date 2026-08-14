package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Проверка создания комментария
    @Test
    void shouldCreateCommentAndSaveItInDatabase() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);

        mockMvc.perform(post("/users/" + user.getId()
                        + "/events/" + event.getId() + "/comments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Комментарий\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Комментарий"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.edited").value(false));

        assertEquals(1, commentRepository.count());
    }

    // Проверка публикации комментария администратором
    @Test
    void shouldPublishCommentByAdmin() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);
        Comment comment = createComment(user, event, CommentStatus.PENDING);

        mockMvc.perform(patch("/admin/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        Comment savedComment = commentRepository.findById(comment.getId()).orElseThrow();

        assertEquals(CommentStatus.PUBLISHED, savedComment.getStatus());
    }

    // Проверка получения опубликованного комментария через публичный API
    @Test
    void shouldGetPublishedCommentThroughPublicApi() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);
        Comment comment = createComment(user, event, CommentStatus.PUBLISHED);

        mockMvc.perform(get("/events/" + event.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(comment.getId()))
                .andExpect(jsonPath("$[0].authorId").value(user.getId()))
                .andExpect(jsonPath("$[0].authorName").value("Пользователь"));

        assertTrue(commentRepository.findById(comment.getId()).isPresent());
    }

    // Проверка отправки опубликованного комментария на повторную модерацию после изменения
    @Test
    void shouldSendPublishedCommentToModerationAfterUpdate() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);
        Comment comment = createComment(user, event, CommentStatus.PUBLISHED);

        mockMvc.perform(patch("/users/" + user.getId()
                        + "/events/" + event.getId()
                        + "/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Изменённый комментарий\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Изменённый комментарий"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.edited").value(true));

        Comment savedComment = commentRepository.findById(comment.getId()).orElseThrow();

        assertEquals(CommentStatus.PENDING, savedComment.getStatus());
        assertTrue(savedComment.getEdited());
    }

    // Проверка получения пользователем своих комментариев с фильтром по статусу
    @Test
    void shouldGetUserCommentsByStatus() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);

        Comment publishedComment = createComment(user, event, CommentStatus.PUBLISHED);
        createComment(user, event, CommentStatus.PENDING);

        mockMvc.perform(get("/users/" + user.getId() + "/comments")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(publishedComment.getId()))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].eventTitle").value("Тестовое событие"));
    }

    // Проверка удаления комментария через API
    @Test
    void shouldDeleteCommentThroughPrivateApi() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);
        Comment comment = createComment(user, event, CommentStatus.PENDING);

        mockMvc.perform(delete("/users/" + user.getId()
                        + "/events/" + event.getId()
                        + "/comments/" + comment.getId()))
                .andExpect(status().isNoContent());

        assertTrue(commentRepository.findById(comment.getId()).isEmpty());
    }

    // Проверка ошибки при передаче commentId от другого события
    @Test
    void shouldReturnNotFoundWhenCommentDoesNotBelongToEvent() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);
        Comment comment = createComment(user, event, CommentStatus.PENDING);

        mockMvc.perform(patch("/users/" + user.getId()
                        + "/events/" + (event.getId() + 1)
                        + "/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Новый текст\"}"))
                .andExpect(status().isNotFound());
    }

    // Проверка повторной модерации ранее опубликованного комментария
    @Test
    void shouldRejectEditedCommentAfterRepeatedModeration() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);
        Comment comment = createComment(user, event, CommentStatus.PUBLISHED);

        mockMvc.perform(patch("/users/" + user.getId()
                        + "/events/" + event.getId()
                        + "/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Первая изменённая версия\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.edited").value(true));

        mockMvc.perform(patch("/admin/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(patch("/users/" + user.getId()
                        + "/events/" + event.getId()
                        + "/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Вторая изменённая версия\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.edited").value(true));

        mockMvc.perform(patch("/admin/comments/" + comment.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.edited").value(true));
    }

    // Проверка пагинации списка комментариев пользователя
    @Test
    void shouldReturnUserCommentsWithPagination() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);

        Comment firstComment = createComment(user, event, CommentStatus.PENDING);
        firstComment.setText("Комментарий1");
        firstComment.setCreatedOn(LocalDateTime.now().minusMinutes(1));
        commentRepository.save(firstComment);

        Comment secondComment = createComment(user, event, CommentStatus.PENDING);
        secondComment.setText("Комментарий2");
        secondComment.setCreatedOn(LocalDateTime.now());
        commentRepository.save(secondComment);

        mockMvc.perform(get("/users/" + user.getId() + "/comments")
                        .param("from", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstComment.getId()));
    }

    // Проверка ошибок валидации текста и размера страницы
    @Test
    void shouldReturnBadRequestForInvalidCommentParameters() throws Exception {
        User user = createUser();
        Event event = createPublishedEvent(user);

        mockMvc.perform(post("/users/" + user.getId()
                        + "/events/" + event.getId() + "/comments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/events/" + event.getId() + "/comments")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private User createUser() {
        User user = new User();
        user.setName("Пользователь");
        user.setEmail("email@mail.ru");

        return userRepository.save(user);
    }

    private Event createPublishedEvent(User user) {
        Category category = new Category();
        category.setName("Категория");
        Category savedCategory = categoryRepository.save(category);

        Event event = new Event();
        event.setAnnotation("Краткое описание события");
        event.setDescription("Подробное описание события");
        event.setTitle("Тестовое событие");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setCategory(savedCategory);
        event.setInitiator(user);
        event.setLat(55.75);
        event.setLon(37.61);
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setState(EventState.PUBLISHED);
        event.setCreatedOn(LocalDateTime.now());
        event.setPublishedOn(LocalDateTime.now());

        return eventRepository.save(event);
    }

    private Comment createComment(User user, Event event, CommentStatus status) {
        Comment comment = new Comment();
        comment.setText("Тестовый комментарий");
        comment.setCreatedOn(LocalDateTime.now());
        comment.setStatus(status);
        comment.setEdited(false);
        comment.setAuthor(user);
        comment.setEvent(event);

        return commentRepository.save(comment);
    }
}