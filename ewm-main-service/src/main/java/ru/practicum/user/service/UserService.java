package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto create(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        User user = userMapper.toUser(request);
        User savedUser = userRepository.save(user);
        log.debug("Пользователь сохранён: id={}", savedUser.getId());

        return userMapper.toUserDto(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository
                    .findAll(PageRequest.of(0, from + size, Sort.by("id")))
                    .getContent();
        } else {
            users = userRepository.findByIdIn(
                    ids,
                    PageRequest.of(0, from + size, Sort.by("id")));
        }

        if (from >= users.size()) {
            return List.of();
        }

        int end = Math.min(from + size, users.size());
        log.debug("Получен список пользователей");

        return userMapper.toUserDtoList(users.subList(from, end));
    }

    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        userRepository.delete(user);
        log.debug("Пользователь удалён: id={}", userId);
    }
}