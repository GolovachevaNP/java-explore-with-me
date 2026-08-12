package ru.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.category.service.CategoryService;
import ru.practicum.exception.ConflictException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryService categoryService;

    // Проверка создания новой категории
    @Test
    void shouldCreateCategory() {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Категория");
        Category category = new Category();
        category.setId(1L);
        category.setName("Категория");
        CategoryDto expected = new CategoryDto(1L, "Категория");
        when(categoryRepository.existsByName("Категория")).thenReturn(false);
        when(categoryMapper.toCategory(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toCategoryDto(category)).thenReturn(expected);

        CategoryDto actual = categoryService.create(request);

        assertEquals(expected, actual);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toCategory(request);
    }

    // Проверка ошибки при создании категории с повторяющимся названием
    @Test
    void shouldThrowExceptionWhenCategoryNameAlreadyExists() {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Категория");
        when(categoryRepository.existsByName("Категория")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.create(request));
        verify(categoryRepository, never()).save(any());
    }

    // Проверка обновления названия категории
    @Test
    void shouldUpdateCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Категория1");
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Категория2");
        CategoryDto expected = new CategoryDto(1L, "Категория2");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Категория2")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toCategoryDto(category)).thenReturn(expected);

        CategoryDto actual = categoryService.update(1L, request);

        assertEquals(expected, actual);
        assertEquals("Категория2", category.getName());
        verify(categoryRepository).save(category);
    }

    // Проверка удаления существующей категории
    @Test
    void shouldDeleteCategory() {
        Category category = new Category();
        category.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    // Проверка ошибки при удалении категории с событиями
    @Test
    void shouldThrowExceptionWhenDeletingCategoryWithEvents() {
        Category category = new Category();
        category.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.delete(1L));

        verify(categoryRepository, never()).delete(category);
    }
}