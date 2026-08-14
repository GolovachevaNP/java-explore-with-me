package ru.practicum.comment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndAuthorIdAndEventId(Long commentId, Long authorId, Long eventId);

    List<Comment> findByEventIdAndStatusOrderByCreatedOnDesc(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByAuthorIdOrderByCreatedOnDesc(Long userId, Pageable pageable);

    List<Comment> findByAuthorIdAndStatusOrderByCreatedOnDesc(Long userId, CommentStatus status, Pageable pageable);

    List<Comment> findAllByOrderByCreatedOnDesc(Pageable pageable);

    List<Comment> findByStatusOrderByCreatedOnDesc(CommentStatus status, Pageable pageable);
}