package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.compilation.controller.AdminCompilationController;
import ru.practicum.compilation.service.CompilationService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    // Проверка удаления подборки через контроллер
    @Test
    void shouldDeleteCompilation() throws Exception {
        mockMvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());
        verify(compilationService).deleteCompilation(1L);
    }
}