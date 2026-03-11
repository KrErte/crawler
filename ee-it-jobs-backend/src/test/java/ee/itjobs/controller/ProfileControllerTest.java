package ee.itjobs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.itjobs.dto.profile.CvBuilderRequest;
import ee.itjobs.dto.profile.ProfileDto;
import ee.itjobs.dto.profile.ProfileUpdateRequest;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.CvBuilderService;
import ee.itjobs.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private CvBuilderService cvBuilderService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private ProfileDto createTestProfile() {
        return ProfileDto.builder()
                .firstName("John").lastName("Doe")
                .email("user@example.com")
                .skills(List.of("Java", "Spring"))
                .hasCv(true)
                .build();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getProfile_returns200() throws Exception {
        when(profileService.getProfile("user@example.com")).thenReturn(createTestProfile());

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(profileService).getProfile("user@example.com");
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void updateProfile_returns200() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        ProfileDto updated = ProfileDto.builder()
                .firstName("Jane").lastName("Smith")
                .email("user@example.com").build();

        when(profileService.updateProfile(eq("user@example.com"), any(ProfileUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));

        verify(profileService).updateProfile(eq("user@example.com"), any(ProfileUpdateRequest.class));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void uploadCv_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "pdf".getBytes());

        when(profileService.uploadCv(eq("user@example.com"), any())).thenReturn(createTestProfile());

        mockMvc.perform(multipart("/api/profile/cv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCv").value(true));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void downloadCv_returns200() throws Exception {
        when(profileService.downloadCv("user@example.com")).thenReturn("pdf bytes".getBytes());

        mockMvc.perform(get("/api/profile/cv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=cv.pdf"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void deleteCv_returns204() throws Exception {
        doNothing().when(profileService).deleteCv("user@example.com");

        mockMvc.perform(delete("/api/profile/cv"))
                .andExpect(status().isNoContent());

        verify(profileService).deleteCv("user@example.com");
    }

    @Test
    void buildCv_returns200() throws Exception {
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("John Doe");
        request.setSkills(List.of("Java"));

        when(cvBuilderService.generateCv(any(CvBuilderRequest.class))).thenReturn("pdf".getBytes());

        mockMvc.perform(post("/api/profile/cv/build")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=cv.pdf"));
    }
}
