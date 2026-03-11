package ee.itjobs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.itjobs.dto.application.ApplicationDto;
import ee.itjobs.dto.application.CreateApplicationRequest;
import ee.itjobs.enums.ApplicationStatus;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private ApplicationDto createTestDto() {
        return ApplicationDto.builder()
                .id(1L).jobId(10L).jobTitle("Java Developer")
                .company("TestCo").status(ApplicationStatus.SUBMITTED)
                .appliedAt(LocalDateTime.now()).build();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getApplications_returns200() throws Exception {
        when(applicationService.getApplications(eq("user@example.com"), isNull()))
                .thenReturn(List.of(createTestDto()));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobTitle").value("Java Developer"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void createApplication_returns200() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setJobId(10L);
        request.setNotes("Interested");

        when(applicationService.createApplication(eq("user@example.com"), any(CreateApplicationRequest.class)))
                .thenReturn(createTestDto());

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(10));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void deleteApplication_returns204() throws Exception {
        doNothing().when(applicationService).deleteApplication("user@example.com", 1L);

        mockMvc.perform(delete("/api/applications/1"))
                .andExpect(status().isNoContent());

        verify(applicationService).deleteApplication("user@example.com", 1L);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void checkApplication_returns200() throws Exception {
        when(applicationService.existsForJob("user@example.com", 10L)).thenReturn(true);

        mockMvc.perform(get("/api/applications/check/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void exportCsv_returns200() throws Exception {
        when(applicationService.exportToCsv("user@example.com")).thenReturn("csv data".getBytes());

        mockMvc.perform(get("/api/applications/export").param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=applications.csv"));
    }
}
