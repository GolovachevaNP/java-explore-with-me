package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Проверка создания события с сохранением в тестовую базу данных
    @Test
    void shouldCreateEventAndSaveItInDatabase() throws Exception {
        Category category = new Category();
        category.setName("Категория");
        Category savedCategory = categoryRepository.save(category);
        User user = new User();
        user.setName("Иван");
        user.setEmail("email@mail.ru");
        User savedUser = userRepository.save(user);
        String request = "{"
                + "\"annotation\":\"Краткое описание события\","
                + "\"category\":" + savedCategory.getId() + ","
                + "\"description\":\"Подробное описание события\","
                + "\"eventDate\":\"2030-01-01 12:00:00\","
                + "\"location\":{\"lat\":55.75,\"lon\":37.61},"
                + "\"paid\":false,"
                + "\"participantLimit\":10,"
                + "\"requestModeration\":true,"
                + "\"title\":\"Событие\""
                + "}";

        mockMvc.perform(post("/users/" + savedUser.getId() + "/events")
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Событие"))
                .andExpect(jsonPath("$.state").value("PENDING"));

        assertEquals(1, eventRepository.count());
    }
}