package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.category.controller.AdminCategoryController;
import ru.practicum.category.controller.PublicCategoryController;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.exception.ErrorHandler;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminCategoryController.class, PublicCategoryController.class})
@Import(ErrorHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    // Проверка создания категории через контроллер
    @Test
    void shouldCreateCategory() throws Exception {
        CategoryDto category = new CategoryDto(1L, "Категория");
        when(categoryService.create(any(NewCategoryDto.class))).thenReturn(category);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Категория\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Категория"));

        verify(categoryService).create(any(NewCategoryDto.class));
    }

    // Проверка ошибки при передаче пустого названия категории
    @Test
    void shouldReturnBadRequestWhenCategoryNameIsEmpty() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400 BAD_REQUEST"));

        verify(categoryService, never()).create(any(NewCategoryDto.class));
    }

    // Проверка получения списка категорий через контроллер
    @Test
    void shouldGetCategories() throws Exception {
        List<CategoryDto> categories = List.of(
                new CategoryDto(1L, "Категория1"),
                new CategoryDto(2L, "Категория2")
        );
        when(categoryService.getAll(0, 10)).thenReturn(categories);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Категория2"));

        verify(categoryService).getAll(0, 10);
    }

    // Проверка ответа при запросе несуществующей категории
    @Test
    void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        when(categoryService.getById(999L))
                .thenThrow(new NotFoundException("Категория с id=999 не найдена"));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404 NOT_FOUND"));
    }

    // Проверка при некорректных данных от сервиса
    @Test
    void shouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() throws Exception {
        doThrow(new IllegalArgumentException("Некорректные данные"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400 BAD_REQUEST"));
    }
}