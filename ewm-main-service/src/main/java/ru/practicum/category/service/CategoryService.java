package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория с таким названием уже существует");
        }

        Category category = mapper.toCategory(dto);
        Category savedCategory = categoryRepository.save(category);
        log.debug("Категория сохранена: id={}", savedCategory.getId());

        return mapper.toCategoryDto(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        List<Category> categories = categoryRepository
                .findAll(PageRequest.of(0, from + size, Sort.by("id")))
                .getContent();

        if (from >= categories.size()) {
            return List.of();
        }

        int end = Math.min(from + size, categories.size());
        log.debug("Получен список категорий");

        return mapper.toCategoryDtoList(categories.subList(from, end));
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(Long categoryId) {
        Category category = findById(categoryId);
        log.debug("Категория найдена: id={}", categoryId);

        return mapper.toCategoryDto(category);
    }

    @Transactional
    public CategoryDto update(Long categoryId, NewCategoryDto dto) {
        Category category = findById(categoryId);

        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория с таким названием уже существует");
        }

        category.setName(dto.getName());
        Category updatedCategory = categoryRepository.save(category);
        log.debug("Категория обновлена: id={}", categoryId);

        return mapper.toCategoryDto(updatedCategory);
    }

    @Transactional
    public void delete(Long categoryId) {
        Category category = findById(categoryId);

        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Нельзя удалить категорию, в которой есть события");
        }

        categoryRepository.delete(category);
        log.debug("Категория удалена: id={}", categoryId);
    }

    private Category findById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
    }
}