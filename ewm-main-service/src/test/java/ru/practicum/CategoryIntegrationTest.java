package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.repository.CategoryRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clearDatabase() {
        categoryRepository.deleteAll();
    }

    // Проверка создания категории с сохранением в тестовую базу данных
    @Test
    void shouldCreateCategoryAndSaveItInDatabase() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Категория\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Категория"));

        assertTrue(categoryRepository.existsByName("Категория"));
    }

    // Проверка получения категории, сохранённой в тестовой базе данных
    @Test
    void shouldGetSavedCategory() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Категория\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Категория"));
    }
}