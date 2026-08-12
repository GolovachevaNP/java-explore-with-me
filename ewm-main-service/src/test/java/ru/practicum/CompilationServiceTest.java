package ru.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.compilation.service.CompilationService;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @Mock
    private EventService eventService;

    @InjectMocks
    private CompilationService compilationService;

    // Проверка создания подборки без событий
    @Test
    void shouldCreateCompilationWithoutEvents() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Категория");
        request.setPinned(true);
        Compilation savedCompilation = new Compilation();
        savedCompilation.setId(1L);
        savedCompilation.setTitle("Категория");
        savedCompilation.setPinned(true);
        CompilationDto expected = new CompilationDto();
        expected.setId(1L);
        expected.setTitle("Категория");
        expected.setPinned(true);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(savedCompilation);
        when(compilationMapper.toCompilationDto(savedCompilation)).thenReturn(expected);
        when(eventService.getEventShortDtos(List.of())).thenReturn(List.of());

        CompilationDto actual = compilationService.createCompilation(request);

        ArgumentCaptor<Compilation> compilationCaptor = ArgumentCaptor.forClass(Compilation.class);
        verify(compilationRepository).save(compilationCaptor.capture());
        assertEquals(expected, actual);
        assertEquals("Категория", compilationCaptor.getValue().getTitle());
        assertTrue(compilationCaptor.getValue().isPinned());
    }

    // Проверка ошибки при создании подборки с несуществующим событием
    @Test
    void shouldThrowExceptionWhenEventDoesNotExist() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Категория");
        request.setEvents(List.of(1L));
        when(eventRepository.findAllById(List.of(1L))).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> compilationService.createCompilation(request));

        verify(compilationRepository, never()).save(any());
    }

    // Проверка обновления названия подборки
    @Test
    void shouldUpdateCompilation() {
        Compilation compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Категория1");
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Категория2");
        CompilationDto expected = new CompilationDto();
        expected.setId(1L);
        expected.setTitle("Категория2");
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.save(compilation)).thenReturn(compilation);
        when(compilationMapper.toCompilationDto(compilation)).thenReturn(expected);
        when(eventService.getEventShortDtos(List.of())).thenReturn(List.of());

        CompilationDto actual = compilationService.updateCompilation(1L, request);

        assertEquals(expected, actual);
        assertEquals("Категория2", compilation.getTitle());
        verify(compilationRepository).save(compilation);
    }

    // Проверка удаления существующей подборки
    @Test
    void shouldDeleteCompilation() {
        Compilation compilation = new Compilation();
        compilation.setId(1L);
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).delete(compilation);
    }
}