package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByIdInAndEventId(List<Long> requestIds, Long eventId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.event.id as eventId, count(r.id) as confirmedRequests "
            + "from ParticipationRequest r "
            + "where r.event.id in :eventIds and r.status = :status "
            + "group by r.event.id")
    List<ConfirmedRequestCount> countConfirmedRequestsByEventIds(
            @Param("eventIds") List<Long> eventIds,
            @Param("status") RequestStatus status
    );
}