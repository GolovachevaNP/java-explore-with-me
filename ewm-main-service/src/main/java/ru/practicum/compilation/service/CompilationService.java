package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventService eventService;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.isPinned());
        compilation.setEvents(getEvents(dto.getEvents()));

        Compilation savedCompilation = compilationRepository.save(compilation);

        log.info("Создана подборка: id={}", savedCompilation.getId());

        return toCompilationDto(savedCompilation);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compilationId + " не найдена"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            compilation.setEvents(getEvents(dto.getEvents()));
        }

        Compilation savedCompilation = compilationRepository.save(compilation);

        log.info("Обновлена подборка: id={}", compilationId);

        return toCompilationDto(savedCompilation);
    }

    @Transactional
    public void deleteCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compilationId + " не найдена"));

        compilationRepository.delete(compilation);

        log.info("Удалена подборка: id={}", compilationId);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Page<Compilation> page;

        if (pinned == null) {
            page = compilationRepository.findAll(PageRequest.of(0, from + size, Sort.by("id")));
        } else {
            page = compilationRepository.findByPinned(
                    pinned,
                    PageRequest.of(0, from + size, Sort.by("id"))
            );
        }

        List<Compilation> compilations = page.getContent();

        if (from >= compilations.size()) {
            return List.of();
        }

        int end = Math.min(from + size, compilations.size());

        List<CompilationDto> result = new ArrayList<>();

        for (Compilation compilation : compilations.subList(from, end)) {
            result.add(toCompilationDto(compilation));
        }

        log.debug("Получен список подборок: количество={}", result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compilationId + " не найдена"));

        log.debug("Получена подборка: id={}", compilationId);

        return toCompilationDto(compilation);
    }

    private Set<Event> getEvents(List<Long> eventIds) {
        if (eventIds == null) {
            return new HashSet<>();
        }

        Set<Long> uniqueEventIds = new HashSet<>(eventIds);

        if (uniqueEventIds.size() != eventIds.size()) {
            throw new IllegalArgumentException("Список событий подборки содержит повторяющиеся id");
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Одно или несколько событий не найдены");
        }

        return new HashSet<>(events);
    }

    private CompilationDto toCompilationDto(Compilation compilation) {
        CompilationDto dto = compilationMapper.toCompilationDto(compilation);
        dto.setEvents(eventService.getEventShortDtos(new ArrayList<>(compilation.getEvents())));

        return dto;
    }
}