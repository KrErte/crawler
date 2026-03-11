package ee.itjobs.controller;

import ee.itjobs.entity.ScrapeRun;
import ee.itjobs.repository.ScrapeRunRepository;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.ScrapeOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScrapeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScrapeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScrapeOrchestratorService orchestratorService;

    @MockBean
    private ScrapeRunRepository scrapeRunRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void triggerScrape_returns200() throws Exception {
        ScrapeRun run = ScrapeRun.builder()
                .id(1L).status("running")
                .startedAt(LocalDateTime.now())
                .triggeredBy("admin").build();

        when(orchestratorService.triggerScrape("admin")).thenReturn(run);

        mockMvc.perform(post("/api/scrape/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void getStatus_notRunning_returns200() throws Exception {
        when(orchestratorService.isRunning()).thenReturn(false);
        when(orchestratorService.getLatestRun()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/scrape/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRunning").value(false));
    }

    @Test
    void getStatus_withLastRun_returns200() throws Exception {
        ScrapeRun run = ScrapeRun.builder()
                .id(1L).status("completed")
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .totalJobs(100).totalNewJobs(20).totalErrors(2)
                .build();

        when(orchestratorService.isRunning()).thenReturn(false);
        when(orchestratorService.getLatestRun()).thenReturn(Optional.of(run));

        mockMvc.perform(get("/api/scrape/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRunning").value(false))
                .andExpect(jsonPath("$.lastRun.totalNewJobs").value(20));
    }

    @Test
    void getScrapeRun_notFound_returns404() throws Exception {
        when(scrapeRunRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/scrape/runs/999"))
                .andExpect(status().isNotFound());
    }
}
