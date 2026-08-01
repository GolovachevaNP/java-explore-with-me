package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может отправить заявку на своё событие");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отправить заявку на неопубликованное событие");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка на это событие уже существует");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(
                eventId,
                RequestStatus.CONFIRMED
        );

        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит заявок на событие");
        }

        ParticipationRequest request = new ParticipationRequest();

        request.setEvent(event);
        request.setRequester(requester);
        request.setCreated(LocalDateTime.now());

        RequestStatus status = event.getParticipantLimit() == 0
                || !event.getRequestModeration()
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;

        request.setStatus(status);

        ParticipationRequest savedRequest = requestRepository.save(request);

        log.debug("Создана заявка: id={}, userId={}, eventId={}", savedRequest.getId(), userId, eventId);

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        log.debug("Получены заявки пользователя: userId={}, количество={}", userId, requests.size());

        return requestMapper.toParticipationRequestDtoList(requests);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository
                .findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        request.setStatus(RequestStatus.CANCELED);

        ParticipationRequest savedRequest = requestRepository.save(request);

        log.info("Пользователь с id={} отменил заявку с id={}", userId, requestId);

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        List<ParticipationRequest> requests = requestRepository.findByEventId(event.getId());

        log.info("Получены заявки на событие с id={}", eventId);

        return requestMapper.toParticipationRequestDtoList(requests);
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (dto.getRequestIds() == null || dto.getRequestIds().isEmpty()) {
            throw new IllegalArgumentException("Нужно передать хотя бы одну заявку");
        }

        Set<Long> uniqueRequestIds = new HashSet<>(dto.getRequestIds());

        if (uniqueRequestIds.size() != dto.getRequestIds().size()) {
            throw new IllegalArgumentException("Список идентификаторов заявок содержит повторяющиеся id");
        }

        if (dto.getStatus() != RequestStatus.CONFIRMED && dto.getStatus() != RequestStatus.REJECTED) {
            throw new IllegalArgumentException("Статус заявки может быть только CONFIRMED или REJECTED");
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Подтверждение заявок для этого события не требуется");
        }

        List<ParticipationRequest> requests = requestRepository.findByIdInAndEventId(dto.getRequestIds(), eventId);

        if (requests.size() != dto.getRequestIds().size()) {
            throw new NotFoundException("Одна или несколько заявок не найдены");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(
                eventId,
                RequestStatus.CONFIRMED
        );

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Изменить можно только заявку в статусе PENDING");
            }

            if (dto.getStatus() == RequestStatus.CONFIRMED) {
                if (confirmedCount >= event.getParticipantLimit()) {
                    throw new ConflictException("Достигнут лимит заявок на событие");
                }

                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmedRequests.add(requestMapper.toParticipationRequestDto(request));
            } else {
                request.setStatus(RequestStatus.REJECTED);

                rejectedRequests.add(requestMapper.toParticipationRequestDto(request));
            }
        }

        requestRepository.saveAll(requests);

        if (dto.getStatus() == RequestStatus.CONFIRMED && confirmedCount == event.getParticipantLimit()) {
            List<ParticipationRequest> allRequests = requestRepository.findByEventId(eventId);

            for (ParticipationRequest request : allRequests) {
                if (request.getStatus() == RequestStatus.PENDING) {
                    request.setStatus(RequestStatus.REJECTED);

                    rejectedRequests.add(requestMapper.toParticipationRequestDto(request));
                }
            }

            requestRepository.saveAll(allRequests);
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmedRequests);
        result.setRejectedRequests(rejectedRequests);

        log.info("Владелец события с id={} изменил статусы заявок", eventId);

        return result;
    }
}