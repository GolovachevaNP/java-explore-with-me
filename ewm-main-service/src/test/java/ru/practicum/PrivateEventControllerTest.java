package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.event.controller.PrivateEventController;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.service.EventService;
import ru.practicum.request.service.RequestService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateEventController.class)
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private RequestService requestService;

    // Проверка получения событий пользователя через контроллер
    @Test
    void shouldGetUserEvents() throws Exception {
        EventShortDto event = new EventShortDto();
        event.setId(1L);
        event.setTitle("Событие");
        when(eventService.getUserEvents(1L, 0, 10)).thenReturn(List.of(event));

        mockMvc.perform(get("/users/1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Событие"));

        verify(eventService).getUserEvents(1L, 0, 10);
    }
}