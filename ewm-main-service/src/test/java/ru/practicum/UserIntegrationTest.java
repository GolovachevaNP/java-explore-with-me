package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAll();
    }

    // Проверка создания пользователя с сохранением в тестовую базу данных
    @Test
    void shouldCreateUserAndSaveItInDatabase() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Пользователь\",\"email\":\"email@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Пользователь"))
                .andExpect(jsonPath("$.email").value("email@mail.ru"));

        assertTrue(userRepository.existsByEmail("email@mail.ru"));
    }
}