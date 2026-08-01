package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.*;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ConfirmedRequestCount;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private static final String STATS_START = "1970-01-01 00:00:00";
    private final ParticipationRequestRepository requestRepository;
    private static final LocalDateTime MIN_EVENT_DATE =
            LocalDateTime.of(1970, 1, 1, 0, 0, 0);

    private static final LocalDateTime MAX_EVENT_DATE =
            LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        LocalDateTime eventDate = parseEventDate(dto.getEventDate());

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Событие должно начинаться не ранее чем через два часа");
        }

        Event event = eventMapper.toEvent(dto);
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setEventDate(eventDate);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        log.debug("Событие сохранено: id={}", savedEvent.getId());

        return toEventFullDto(savedEvent);
    }

    private EventFullDto toEventFullDto(Event event) {
        return toEventFullDto(event, getConfirmedRequests(event.getId()));
    }

    private EventFullDto toEventFullDto(Event event, long confirmedRequests) {
        EventFullDto dto = eventMapper.toEventFullDto(event);

        dto.setConfirmedRequests(confirmedRequests);
        dto.setLocation(eventMapper.toLocation(event));
        dto.setViews(0L);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        List<Event> events = eventRepository.findByInitiatorId(
                userId,
                PageRequest.of(0, from + size, Sort.by("id"))
        );
        events = getEventPage(events, from, size);
        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = eventMapper.toEventShortDto(event);
            dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
            dto.setViews(0L);
            result.add(dto);
        }

        log.debug("Получены события пользователя: userId={}, количество={}", userId, result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        log.debug("Найдено событие пользователя: userId={}, eventId={}", userId, eventId);

        return toEventFullDto(event);
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Опубликованное событие нельзя изменить");
        }

        Event changes = eventMapper.toEvent(dto);

        if (dto.getAnnotation() != null) {
            event.setAnnotation(changes.getAnnotation());
        }

        if (dto.getDescription() != null) {
            event.setDescription(changes.getDescription());
        }

        if (dto.getTitle() != null) {
            event.setTitle(changes.getTitle());
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

            event.setCategory(category);
        }

        if (dto.getLocation() != null) {
            event.setLat(changes.getLat());
            event.setLon(changes.getLon());
        }

        if (dto.getPaid() != null) {
            event.setPaid(changes.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(changes.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(changes.getRequestModeration());
        }

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = parseEventDate(dto.getEventDate());

            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("Событие должно начинаться не ранее чем через два часа");
            }

            event.setEventDate(eventDate);
        }

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else {
                event.setState(EventState.CANCELED);
            }
        }

        Event savedEvent = eventRepository.save(event);

        log.debug("Событие обновлено: id={}, userId={}", eventId, userId);

        return toEventFullDto(savedEvent);
    }

    @Transactional
    public EventFullDto updateAdminEvent(
            Long eventId,
            UpdateEventAdminRequest dto) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        Event changes = eventMapper.toEvent(dto);

        if (dto.getAnnotation() != null) {
            event.setAnnotation(changes.getAnnotation());
        }

        if (dto.getDescription() != null) {
            event.setDescription(changes.getDescription());
        }

        if (dto.getTitle() != null) {
            event.setTitle(changes.getTitle());
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

            event.setCategory(category);
        }

        if (dto.getLocation() != null) {
            event.setLat(changes.getLat());
            event.setLon(changes.getLon());
        }

        if (dto.getPaid() != null) {
            event.setPaid(changes.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(changes.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(changes.getRequestModeration());
        }

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = parseEventDate(dto.getEventDate());

            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Событие должно начинаться не ранее чем через один час");
            }

            event.setEventDate(eventDate);
        }

        if (dto.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Можно опубликовать только событие в состоянии ожидания");
            }

            if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Событие должно начинаться не ранее чем через один час");
            }

            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }

        if (dto.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя отклонить опубликованное событие");
            }

            event.setState(EventState.CANCELED);
        }

        Event savedEvent = eventRepository.save(event);

        log.debug("Событие изменено администратором: id={}", eventId);

        return toEventFullDto(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            boolean onlyAvailable,
            String rangeStart,
            String rangeEnd,
            EventSort sort,
            int from,
            int size
    ) {
        String searchText;

        if (text == null || text.isBlank()) {
            searchText = "";
        } else {
            searchText = text;
        }

        boolean filterByCategories = categories != null && !categories.isEmpty();

        List<Long> categoryIds;

        if (filterByCategories) {
            categoryIds = categories;
        } else {
            categoryIds = List.of(-1L);
        }

        boolean filterByPaid = paid != null;
        boolean paidValue = paid != null && paid;

        LocalDateTime start;

        if (rangeStart == null) {
            start = LocalDateTime.now();
        } else {
            start = parseEventDate(rangeStart);
        }

        boolean filterByRangeEnd = rangeEnd != null;

        LocalDateTime end;

        if (filterByRangeEnd) {
            end = parseEventDate(rangeEnd);
        } else {
            end = LocalDateTime.of(3000, 1, 1, 0, 0, 0);
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Pageable pageable;

        if (sort == EventSort.VIEWS) {
            pageable = Pageable.unpaged();
        } else {
            pageable = PageRequest.of(0, from + size, Sort.by("eventDate"));
        }

        List<Event> events = eventRepository.findPublicEvents(
                EventState.PUBLISHED,
                searchText,
                filterByCategories,
                categoryIds,
                filterByPaid,
                paidValue,
                start,
                filterByRangeEnd,
                end,
                onlyAvailable,
                RequestStatus.CONFIRMED,
                pageable
        ).getContent();

        if (sort != EventSort.VIEWS) {
            events = getEventPage(events, from, size);
        }

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        List<ViewStatsDto> statistics = getStatistics(events);
        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = eventMapper.toEventShortDto(event);

            dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
            dto.setViews(getViewsFromStatistics(event.getId(), statistics));

            result.add(dto);
        }

        if (sort == EventSort.VIEWS) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());

            return getEventShortDtoPage(result, from, size);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = eventRepository.findByIdAndState(
                eventId,
                EventState.PUBLISHED
        ).orElseThrow(() -> new NotFoundException("Опубликованное событие с id=" + eventId + " не найдено"));

        log.debug("Получено опубликованное событие: id={}", eventId);

        EventFullDto dto = toEventFullDto(event);
        dto.setViews(getViews(eventId));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            String rangeStart,
            String rangeEnd,
            int from,
            int size
    ) {
        boolean filterByUsers = users != null && !users.isEmpty();

        List<Long> userIds;

        if (filterByUsers) {
            userIds = users;
        } else {
            userIds = List.of(-1L);
        }

        boolean filterByStates = states != null && !states.isEmpty();

        List<EventState> eventStates;

        if (filterByStates) {
            eventStates = states;
        } else {
            eventStates = List.of(EventState.PENDING);
        }

        boolean filterByCategories = categories != null && !categories.isEmpty();

        List<Long> categoryIds;

        if (filterByCategories) {
            categoryIds = categories;
        } else {
            categoryIds = List.of(-1L);
        }

        boolean filterByRangeStart = rangeStart != null;

        LocalDateTime start;

        if (filterByRangeStart) {
            start = parseEventDate(rangeStart);
        } else {
            start = MIN_EVENT_DATE;
        }

        boolean filterByRangeEnd = rangeEnd != null;

        LocalDateTime end;

        if (filterByRangeEnd) {
            end = parseEventDate(rangeEnd);
        } else {
            end = MAX_EVENT_DATE;
        }

        if (filterByRangeStart && filterByRangeEnd && start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Event> events = eventRepository.findAdminEvents(
                filterByUsers,
                userIds,
                filterByStates,
                eventStates,
                filterByCategories,
                categoryIds,
                filterByRangeStart,
                start,
                filterByRangeEnd,
                end,
                PageRequest.of(0, from + size, Sort.by("id"))
        ).getContent();
        events = getEventPage(events, from, size);
        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        List<EventFullDto> result = new ArrayList<>();

        for (Event event : events) {
            result.add(toEventFullDto(event, confirmedRequests.getOrDefault(event.getId(), 0L)));
        }

        log.debug("Администратор получил список событий: количество={}", result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public EventShortDto getEventShortDto(Event event) {
        return getEventShortDtos(List.of(event)).getFirst();
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getEventShortDtos(List<Event> events) {
        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);
        List<ViewStatsDto> statistics = getStatistics(events);
        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = eventMapper.toEventShortDto(event);
            dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
            dto.setViews(getViewsFromStatistics(event.getId(), statistics));
            result.add(dto);
        }

        return result;
    }

    private List<ViewStatsDto> getStatistics(List<Event> events) {
        List<String> uris = new ArrayList<>();

        for (Event event : events) {
            uris.add("/events/" + event.getId());
        }

        if (uris.isEmpty()) {
            return new ArrayList<>();
        }

        return statsClient.getStats(STATS_START, LocalDateTime.now().format(FORMATTER), uris, true);
    }

    private long getViewsFromStatistics(Long eventId, List<ViewStatsDto> statistics) {
        String uri = "/events/" + eventId;

        for (ViewStatsDto statistic : statistics) {
            if (statistic.getUri().equals(uri)) {
                return statistic.getHits();
            }
        }

        return 0L;
    }

    private long getViews(Long eventId) {
        List<String> uris = new ArrayList<>();
        uris.add("/events/" + eventId);

        List<ViewStatsDto> statistics = statsClient.getStats(
                STATS_START, LocalDateTime.now().format(FORMATTER), uris, true);

        if (statistics.isEmpty()) {
            return 0L;
        }

        return statistics.getFirst().getHits();
    }

    private List<Event> getEventPage(List<Event> events, int from, int size) {
        if (from >= events.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(from + size, events.size());

        return new ArrayList<>(events.subList(from, end));
    }

    private List<EventShortDto> getEventShortDtoPage(List<EventShortDto> events, int from, int size) {
        if (from >= events.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(from + size, events.size());

        return new ArrayList<>(events.subList(from, end));
    }

    private LocalDateTime parseEventDate(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }

    private long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        Map<Long, Long> confirmedRequests = new HashMap<>();
        List<Long> eventIds = new ArrayList<>();

        for (Event event : events) {
            eventIds.add(event.getId());
        }

        if (eventIds.isEmpty()) {
            return confirmedRequests;
        }

        List<ConfirmedRequestCount> requestCounts = requestRepository
                .countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED);

        for (ConfirmedRequestCount requestCount : requestCounts) {
            confirmedRequests.put(requestCount.getEventId(), requestCount.getConfirmedRequests());
        }

        return confirmedRequests;
    }
}