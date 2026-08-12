package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.request.controller.PrivateRequestController;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.service.RequestService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateRequestController.class)
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    // Проверка получения заявок пользователя через контроллер
    @Test
    void shouldGetRequests() throws Exception {
        ParticipationRequestDto request = new ParticipationRequestDto();
        request.setId(1L);
        request.setStatus(RequestStatus.PENDING);
        when(requestService.getUserRequests(1L)).thenReturn(List.of(request));

        mockMvc.perform(get("/users/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(requestService).getUserRequests(1L);
    }
}