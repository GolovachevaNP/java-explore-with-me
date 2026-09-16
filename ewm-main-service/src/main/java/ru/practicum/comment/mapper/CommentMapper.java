package ru.practicum.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.comment.dto.AdminCommentDto;
import ru.practicum.comment.dto.PublicCommentDto;
import ru.practicum.comment.dto.UserCommentDto;
import ru.practicum.comment.model.Comment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    // Преобразует комментарий в DTO для автора комментария
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    UserCommentDto toUserCommentDto(Comment comment);

    List<UserCommentDto> toUserCommentDtoList(List<Comment> comments);

    // Преобразует комментарий в DTO для публичного API
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.name")
    PublicCommentDto toPublicCommentDto(Comment comment);

    List<PublicCommentDto> toPublicCommentDtoList(List<Comment> comments);

    // Преобразует комментарий в DTO для администратора
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "authorId", source = "author.id")
    AdminCommentDto toAdminCommentDto(Comment comment);

    List<AdminCommentDto> toAdminCommentDtoList(List<Comment> comments);
}