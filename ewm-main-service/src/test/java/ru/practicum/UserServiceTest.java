package ru.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.exception.ConflictException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.user.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // Проверка создания нового пользователя
    @Test
    void shouldCreateUser() {
        NewUserRequest request = new NewUserRequest();
        request.setName("Пользователь");
        request.setEmail("email@mail.ru");
        User user = new User();
        user.setId(1L);
        user.setName("Пользователь");
        user.setEmail("email@mail.ru");
        UserDto expected = new UserDto();
        expected.setId(1L);
        expected.setName("Пользователь");
        expected.setEmail("email@mail.ru");
        when(userRepository.existsByEmail("email@mail.ru")).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expected);

        UserDto actual = userService.create(request);

        assertEquals(expected, actual);
        verify(userRepository).save(user);
        verify(userMapper).toUser(request);
    }

    // Проверка ошибки при создании пользователя с повторяющимся email
    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("email@mail.ru");
        when(userRepository.existsByEmail("email@mail.ru")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.create(request));

        verify(userRepository, never()).save(any());
    }

    // Проверка удаления существующего пользователя
    @Test
    void shouldDeleteUser() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }
}