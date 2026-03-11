package ee.itjobs.controller;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.match.JobMatchScoreDto;
import ee.itjobs.dto.match.MatchResultDto;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void match_withFile_returns200() throws Exception {
        JobDto jobDto = JobDto.builder()
                .id(1L).title("Java Dev").company("TestCo").url("https://example.com/1").build();
        MatchResultDto result = MatchResultDto.builder()
                .job(jobDto).matchPercentage(85).build();

        when(matchService.matchJobs(any(byte[].class), eq(20))).thenReturn(List.of(result));

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/api/match").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchPercentage").value(85))
                .andExpect(jsonPath("$[0].job.title").value("Java Dev"));

        verify(matchService).matchJobs(any(byte[].class), eq(20));
    }

    @Test
    void matchFromProfile_returns200() throws Exception {
        JobDto jobDto = JobDto.builder()
                .id(1L).title("Python Dev").company("TechCo").build();
        MatchResultDto result = MatchResultDto.builder()
                .job(jobDto).matchPercentage(72).build();

        when(matchService.matchJobsFromProfile(eq("user@example.com"), eq(20))).thenReturn(List.of(result));

        mockMvc.perform(post("/api/match/profile")
                        .principal(() -> "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchPercentage").value(72));

        verify(matchService).matchJobsFromProfile("user@example.com", 20);
    }

    @Test
    void getMatchScores_returns200() throws Exception {
        JobMatchScoreDto score = JobMatchScoreDto.builder()
                .jobId(1L).matchPercentage(65).build();

        when(matchService.matchJobsByIds(eq("user@example.com"), anyList())).thenReturn(List.of(score));

        mockMvc.perform(post("/api/match/scores")
                        .principal(() -> "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(1))
                .andExpect(jsonPath("$[0].matchPercentage").value(65));

        verify(matchService).matchJobsByIds(eq("user@example.com"), anyList());
    }
}
