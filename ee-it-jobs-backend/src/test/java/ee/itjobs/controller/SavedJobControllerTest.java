package ee.itjobs.controller;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.SavedJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavedJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class SavedJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SavedJobService savedJobService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "user@example.com")
    void getSavedJobs_returns200() throws Exception {
        JobDto job = JobDto.builder().id(1L).title("Java Dev").company("TestCo").build();
        when(savedJobService.getSavedJobs("user@example.com")).thenReturn(List.of(job));

        mockMvc.perform(get("/api/saved-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java Dev"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getSavedJobIds_returns200() throws Exception {
        when(savedJobService.getSavedJobIds("user@example.com")).thenReturn(Set.of(1L, 2L, 3L));

        mockMvc.perform(get("/api/saved-jobs/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void saveJob_returns201() throws Exception {
        doNothing().when(savedJobService).saveJob("user@example.com", 10L);

        mockMvc.perform(post("/api/saved-jobs/10"))
                .andExpect(status().isCreated());

        verify(savedJobService).saveJob("user@example.com", 10L);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void unsaveJob_returns204() throws Exception {
        doNothing().when(savedJobService).unsaveJob("user@example.com", 10L);

        mockMvc.perform(delete("/api/saved-jobs/10"))
                .andExpect(status().isNoContent());

        verify(savedJobService).unsaveJob("user@example.com", 10L);
    }
}
