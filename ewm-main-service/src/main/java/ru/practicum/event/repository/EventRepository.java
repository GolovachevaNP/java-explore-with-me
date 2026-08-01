package ru.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.request.model.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    boolean existsByCategoryId(Long categoryId);

    @Query("select e from Event e "
            + "where e.state = :state "
            + "and (:text = '' or "
            + "lower(e.annotation) like lower(concat('%', :text, '%')) or "
            + "lower(e.description) like lower(concat('%', :text, '%'))) "
            + "and (:filterByCategories = false or e.category.id in :categories) "
            + "and (:filterByPaid = false or e.paid = :paid) "
            + "and e.eventDate >= :rangeStart "
            + "and (:filterByRangeEnd = false or e.eventDate <= :rangeEnd) "
            + "and (:onlyAvailable = false or e.participantLimit = 0 or "
            + "e.participantLimit > (select count(r.id) from ParticipationRequest r "
            + "where r.event.id = e.id and r.status = :confirmedStatus))")
    Page<Event> findPublicEvents(
            @Param("state") EventState state,
            @Param("text") String text,
            @Param("filterByCategories") boolean filterByCategories,
            @Param("categories") List<Long> categories,
            @Param("filterByPaid") boolean filterByPaid,
            @Param("paid") boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("filterByRangeEnd") boolean filterByRangeEnd,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") boolean onlyAvailable,
            @Param("confirmedStatus") RequestStatus confirmedStatus,
            Pageable pageable
    );

    @Query("select e from Event e "
            + "where (:filterByUsers = false or e.initiator.id in :users) "
            + "and (:filterByStates = false or e.state in :states) "
            + "and (:filterByCategories = false or e.category.id in :categories) "
            + "and (:filterByRangeStart = false or e.eventDate >= :rangeStart) "
            + "and (:filterByRangeEnd = false or e.eventDate <= :rangeEnd)")
    Page<Event> findAdminEvents(
            @Param("filterByUsers") boolean filterByUsers,
            @Param("users") List<Long> users,
            @Param("filterByStates") boolean filterByStates,
            @Param("states") List<EventState> states,
            @Param("filterByCategories") boolean filterByCategories,
            @Param("categories") List<Long> categories,
            @Param("filterByRangeStart") boolean filterByRangeStart,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("filterByRangeEnd") boolean filterByRangeEnd,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );
}