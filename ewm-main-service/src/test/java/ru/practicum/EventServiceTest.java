package ru.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.AdminStateAction;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ConfirmedRequestCount;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventService eventService;

    // Проверка создания нового события
    @Test
    void shouldCreateEvent() {
        NewEventDto request = new NewEventDto();
        request.setCategory(1L);
        request.setEventDate("2030-01-01 12:00:00");
        Category category = new Category();
        category.setId(1L);
        User user = new User();
        user.setId(2L);
        Event event = new Event();
        event.setId(3L);
        EventFullDto expected = new EventFullDto();
        expected.setId(3L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(eventMapper.toEvent(request)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toEventFullDto(event)).thenReturn(expected);
        when(requestRepository.countByEventIdAndStatus(3L, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventFullDto actual = eventService.create(2L, request);

        assertEquals(expected, actual);
        assertEquals(EventState.PENDING, event.getState());
        assertEquals(category, event.getCategory());
        assertEquals(user, event.getInitiator());
        verify(eventRepository).save(event);
    }

    // Проверка ошибки при создании события с несуществующей категорией
    @Test
    void shouldThrowExceptionWhenCategoryDoesNotExist() {
        NewEventDto request = new NewEventDto();
        request.setCategory(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.create(2L, request));
    }

    // Проверка изменения названия события пользователем
    @Test
    void shouldUpdateUserEvent() {
        Event event = new Event();
        event.setId(3L);
        event.setState(EventState.PENDING);
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Новое название");
        Event changes = new Event();
        changes.setTitle("Новое название");
        EventFullDto expected = new EventFullDto();
        expected.setId(3L);
        when(eventRepository.findByIdAndInitiatorId(3L, 2L)).thenReturn(Optional.of(event));
        when(eventMapper.toEvent(request)).thenReturn(changes);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toEventFullDto(event)).thenReturn(expected);
        when(requestRepository.countByEventIdAndStatus(3L, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventFullDto actual = eventService.updateUserEvent(2L, 3L, request);

        assertEquals(expected, actual);
        assertEquals("Новое название", event.getTitle());
        verify(eventRepository).save(event);
    }

    // Проверка публикации события администратором
    @Test
    void shouldPublishEventByAdmin() {
        Event event = new Event();
        event.setId(3L);
        event.setState(EventState.PENDING);
        event.setEventDate(java.time.LocalDateTime.of(2030, 1, 1, 12, 0));
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminStateAction.PUBLISH_EVENT);
        EventFullDto expected = new EventFullDto();
        expected.setId(3L);
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(eventMapper.toEvent(request)).thenReturn(new Event());
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toEventFullDto(event)).thenReturn(expected);
        when(requestRepository.countByEventIdAndStatus(3L, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventFullDto actual = eventService.updateAdminEvent(3L, request);

        assertEquals(expected, actual);
        assertEquals(EventState.PUBLISHED, event.getState());
        verify(eventRepository).save(event);
    }

    // Проверка получения количества заявок одним запросом для списка событий пользователя
    @Test
    void shouldGetConfirmedRequestsForUserEventsInOneRequest() {
        User user = new User();
        user.setId(2L);
        Event event = new Event();
        event.setId(3L);
        EventShortDto expected = new EventShortDto();
        expected.setId(3L);
        ConfirmedRequestCount requestCount = org.mockito.Mockito.mock(ConfirmedRequestCount.class);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(eventRepository.findByInitiatorId(eq(2L), any())).thenReturn(List.of(event));
        when(requestRepository.countConfirmedRequestsByEventIds(List.of(3L), RequestStatus.CONFIRMED))
                .thenReturn(List.of(requestCount));
        when(requestCount.getEventId()).thenReturn(3L);
        when(requestCount.getConfirmedRequests()).thenReturn(2L);
        when(eventMapper.toEventShortDto(event)).thenReturn(expected);

        List<EventShortDto> actual = eventService.getUserEvents(2L, 0, 10);

        assertEquals(1, actual.size());
        assertEquals(2L, actual.getFirst().getConfirmedRequests());
        verify(requestRepository).countConfirmedRequestsByEventIds(List.of(3L), RequestStatus.CONFIRMED);
        verify(requestRepository, never()).countByEventIdAndStatus(3L, RequestStatus.CONFIRMED);
    }
}