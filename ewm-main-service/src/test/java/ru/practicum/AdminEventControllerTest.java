package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.event.controller.AdminEventController;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.service.EventService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    // Проверка получения событий администратором через контроллер
    @Test
    void shouldGetEvents() throws Exception {
        EventFullDto event = new EventFullDto();
        event.setId(1L);
        event.setTitle("Событие");
        when(eventService.getAdminEvents(null, null, null, null, null, 0, 10)).thenReturn(List.of(event));

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Событие"));

        verify(eventService).getAdminEvents(null, null, null, null, null, 0, 10);
    }

    // Проверка валидации короткого заголовка при обновлении события администратором
    @Test
    void shouldRejectShortTitleWhenUpdatingEvent() throws Exception {
        mockMvc.perform(patch("/admin/events/1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"ab\"}"))
                .andExpect(status().isBadRequest());
    }
}