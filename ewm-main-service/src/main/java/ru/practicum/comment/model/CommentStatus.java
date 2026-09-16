package ru.practicum.comment.model;

public enum CommentStatus {
    PENDING,   // комментарий ожидает проверки администратора и не виден публично
    PUBLISHED, // комментарий одобрен и доступен в публичном списке
    REJECTED   // комментарий отклонён и не доступен публично
}