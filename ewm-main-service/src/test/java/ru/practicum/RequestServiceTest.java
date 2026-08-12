package ru.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.request.service.RequestService;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private RequestService requestService;

    // Проверка создания заявки на опубликованное событие
    @Test
    void shouldCreatePendingRequest() {
        User requester = new User();
        requester.setId(2L);
        User initiator = new User();
        initiator.setId(1L);
        Event event = new Event();
        event.setId(3L);
        event.setInitiator(initiator);
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);
        ParticipationRequestDto expected = new ParticipationRequestDto();
        expected.setId(4L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(3L, 2L)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(3L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
                    ParticipationRequest request = invocation.getArgument(0);
                    request.setId(4L);
                    return request;
                });
        when(requestMapper.toParticipationRequestDto(any(ParticipationRequest.class))).thenReturn(expected);

        ParticipationRequestDto actual = requestService.createRequest(2L, 3L);

        ArgumentCaptor<ParticipationRequest> requestCaptor = ArgumentCaptor.forClass(ParticipationRequest.class);
        verify(requestRepository).save(requestCaptor.capture());
        assertEquals(expected, actual);
        assertEquals(RequestStatus.PENDING, requestCaptor.getValue().getStatus());
        assertEquals(event, requestCaptor.getValue().getEvent());
        assertEquals(requester, requestCaptor.getValue().getRequester());
    }

    // Проверка ошибки при создании заявки инициатором своего события
    @Test
    void shouldThrowExceptionWhenInitiatorCreatesRequest() {
        User user = new User();
        user.setId(1L);
        Event event = new Event();
        event.setId(3L);
        event.setInitiator(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class, () -> requestService.createRequest(1L, 3L));
    }

    // Проверка отмены своей заявки
    @Test
    void shouldCancelRequest() {
        ParticipationRequest request = new ParticipationRequest();
        request.setId(4L);
        request.setStatus(RequestStatus.PENDING);
        ParticipationRequestDto expected = new ParticipationRequestDto();
        expected.setId(4L);
        expected.setStatus(RequestStatus.CANCELED);
        when(requestRepository.findByIdAndRequesterId(4L, 2L)).thenReturn(Optional.of(request));
        when(requestRepository.save(request)).thenReturn(request);
        when(requestMapper.toParticipationRequestDto(request)).thenReturn(expected);

        ParticipationRequestDto actual = requestService.cancelRequest(2L, 4L);

        assertEquals(expected, actual);
        assertEquals(RequestStatus.CANCELED, request.getStatus());
        verify(requestRepository).save(request);
    }

    // Проверка отклонения ожидающей заявки после заполнения лимита участников
    @Test
    void shouldRejectPendingRequestWhenParticipantLimitIsReached() {
        User initiator = new User();
        initiator.setId(1L);
        Event event = new Event();
        event.setId(3L);
        event.setInitiator(initiator);
        event.setParticipantLimit(1);
        event.setRequestModeration(true);
        ParticipationRequest confirmedRequest = new ParticipationRequest();
        confirmedRequest.setId(4L);
        confirmedRequest.setStatus(RequestStatus.PENDING);
        ParticipationRequest rejectedRequest = new ParticipationRequest();
        rejectedRequest.setId(5L);
        rejectedRequest.setStatus(RequestStatus.PENDING);
        ParticipationRequestDto confirmedDto = new ParticipationRequestDto();
        confirmedDto.setId(4L);
        confirmedDto.setStatus(RequestStatus.CONFIRMED);
        ParticipationRequestDto rejectedDto = new ParticipationRequestDto();
        rejectedDto.setId(5L);
        rejectedDto.setStatus(RequestStatus.REJECTED);
        EventRequestStatusUpdateRequest request = new EventRequestStatusUpdateRequest();
        request.setRequestIds(List.of(4L));
        request.setStatus(RequestStatus.CONFIRMED);
        when(eventRepository.findByIdAndInitiatorId(3L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByIdInAndEventId(List.of(4L), 3L)).thenReturn(List.of(confirmedRequest));
        when(requestRepository.countByEventIdAndStatus(3L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.findByEventId(3L)).thenReturn(List.of(confirmedRequest, rejectedRequest));
        when(requestMapper.toParticipationRequestDto(confirmedRequest)).thenReturn(confirmedDto);
        when(requestMapper.toParticipationRequestDto(rejectedRequest)).thenReturn(rejectedDto);

        EventRequestStatusUpdateResult result = requestService.changeRequestStatus(1L, 3L, request);

        assertEquals(RequestStatus.CONFIRMED, confirmedRequest.getStatus());
        assertEquals(RequestStatus.REJECTED, rejectedRequest.getStatus());
        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals(1, result.getRejectedRequests().size());
        verify(requestRepository).saveAll(List.of(confirmedRequest));
        verify(requestRepository).saveAll(List.of(confirmedRequest, rejectedRequest));
    }
}