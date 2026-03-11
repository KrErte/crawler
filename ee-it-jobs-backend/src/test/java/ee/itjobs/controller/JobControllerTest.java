package ee.itjobs.controller;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.job.JobFilterDto;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getJobs_returnsPage() throws Exception {
        // Arrange
        JobDto jobDto = JobDto.builder()
                .id(1L)
                .title("Java Developer")
                .company("TestCo")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .build();

        Page<JobDto> page = new PageImpl<>(List.of(jobDto), PageRequest.of(0, 20), 1);

        when(jobService.getJobs(
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                anyInt(), anyInt()
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/jobs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java Developer"))
                .andExpect(jsonPath("$.content[0].company").value("TestCo"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getJob_returnsJob() throws Exception {
        // Arrange
        JobDto jobDto = JobDto.builder()
                .id(1L)
                .title("Python Developer")
                .company("DataCorp")
                .url("https://example.com/job/1")
                .source("cv.ee")
                .descriptionSnippet("We are looking for a Python developer")
                .build();

        when(jobService.getJob(1L)).thenReturn(jobDto);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Python Developer"))
                .andExpect(jsonPath("$.company").value("DataCorp"))
                .andExpect(jsonPath("$.source").value("cv.ee"));
    }

    @Test
    void getFilters_returnsFilters() throws Exception {
        // Arrange
        JobFilterDto filterDto = JobFilterDto.builder()
                .companies(List.of("CompanyA", "CompanyB"))
                .sources(List.of("cvkeskus", "cv.ee"))
                .jobTypes(List.of("FULL_TIME", "PART_TIME"))
                .workplaceTypes(List.of("REMOTE", "ONSITE"))
                .build();

        when(jobService.getFilters()).thenReturn(filterDto);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies").isArray())
                .andExpect(jsonPath("$.companies[0]").value("CompanyA"))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0]").value("cvkeskus"))
                .andExpect(jsonPath("$.jobTypes").isArray())
                .andExpect(jsonPath("$.workplaceTypes").isArray());
    }
}
