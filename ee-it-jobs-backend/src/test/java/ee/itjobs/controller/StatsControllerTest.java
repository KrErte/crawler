package ee.itjobs.controller;

import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getTopSkills_returns200() throws Exception {
        when(statsService.getTopSkills(20)).thenReturn(List.of(
                Map.of("skill", "Java", "count", 50),
                Map.of("skill", "Python", "count", 35)
        ));

        mockMvc.perform(get("/api/stats/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skill").value("Java"))
                .andExpect(jsonPath("$[0].count").value(50));
    }

    @Test
    void getJobsBySource_returns200() throws Exception {
        when(statsService.getJobsBySource()).thenReturn(List.of(
                Map.of("source", "cvkeskus", "count", 120)
        ));

        mockMvc.perform(get("/api/stats/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("cvkeskus"));
    }

    @Test
    void getDailyTrends_returns200() throws Exception {
        when(statsService.getDailyJobTrends(30)).thenReturn(List.of(
                Map.of("date", "2025-01-15", "count", 25)
        ));

        mockMvc.perform(get("/api/stats/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(25));
    }

    @Test
    void getTopSkills_withCustomLimit_returns200() throws Exception {
        when(statsService.getTopSkills(5)).thenReturn(List.of(
                Map.of("skill", "JavaScript", "count", 80)
        ));

        mockMvc.perform(get("/api/stats/skills").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skill").value("JavaScript"));

        verify(statsService).getTopSkills(5);
    }
}
