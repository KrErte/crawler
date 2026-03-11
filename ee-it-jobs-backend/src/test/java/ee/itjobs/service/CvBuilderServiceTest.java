package ee.itjobs.service;

import ee.itjobs.dto.profile.CvBuilderRequest;
import ee.itjobs.dto.profile.ProfileDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CvBuilderServiceTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private CvBuilderService cvBuilderService;

    @Test
    void generateCv_minimalData_returnsPdfBytes() throws IOException {
        // Arrange
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("John Doe");

        // Act
        byte[] result = cvBuilderService.generateCv(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        // PDF files start with %PDF
        String header = new String(result, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    void generateCv_allSections_returnsPdfBytes() throws IOException {
        // Arrange
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("Jane Smith");
        request.setEmail("jane@example.com");
        request.setPhone("+372 5551234");
        request.setLinkedinUrl("https://linkedin.com/in/janesmith");
        request.setSummary("Experienced software engineer with 10 years of experience in Java and Spring Boot.");

        CvBuilderRequest.ExperienceEntry exp = new CvBuilderRequest.ExperienceEntry();
        exp.setCompany("Acme Corp");
        exp.setRole("Senior Developer");
        exp.setStartDate("2020-01");
        exp.setEndDate("2024-06");
        exp.setDescription("Led a team of 5 developers building microservices.");
        request.setExperience(List.of(exp));

        CvBuilderRequest.EducationEntry edu = new CvBuilderRequest.EducationEntry();
        edu.setInstitution("University of Tartu");
        edu.setDegree("MSc");
        edu.setField("Computer Science");
        edu.setStartDate("2014");
        edu.setEndDate("2016");
        request.setEducation(List.of(edu));

        request.setSkills(List.of("Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes"));

        // Act
        byte[] result = cvBuilderService.generateCv(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        String header = new String(result, 0, 4);
        assertEquals("%PDF", header);
        // Full CV with all sections should be larger than minimal
        assertTrue(result.length > 500, "Full CV PDF should be larger than 500 bytes");
    }

    @Test
    void generateCv_pdfBytesStartWithPdfHeader() throws IOException {
        // Arrange
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setSummary("A brief summary.");

        // Act
        byte[] result = cvBuilderService.generateCv(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.length >= 4);
        assertEquals((byte) '%', result[0]);
        assertEquals((byte) 'P', result[1]);
        assertEquals((byte) 'D', result[2]);
        assertEquals((byte) 'F', result[3]);
    }

    @Test
    void generateAndSaveCv_callsProfileServiceUploadCv() throws IOException {
        // Arrange
        String email = "jane@example.com";
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("Jane Smith");
        request.setEmail(email);
        request.setSkills(List.of("Java", "Python"));

        ProfileDto expectedProfile = ProfileDto.builder()
                .email(email)
                .firstName("Jane")
                .lastName("Smith")
                .hasCv(true)
                .build();

        when(profileService.uploadCv(eq(email), any(MultipartFile.class))).thenReturn(expectedProfile);

        // Act
        ProfileDto result = cvBuilderService.generateAndSaveCv(email, request);

        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertTrue(result.isHasCv());

        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(profileService).uploadCv(eq(email), fileCaptor.capture());

        MultipartFile capturedFile = fileCaptor.getValue();
        assertEquals("cv.pdf", capturedFile.getOriginalFilename());
        assertEquals("application/pdf", capturedFile.getContentType());
        assertFalse(capturedFile.isEmpty());
        // Verify the uploaded file content is a valid PDF
        byte[] fileBytes = capturedFile.getBytes();
        String header = new String(fileBytes, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    void generateCv_nullOptionalFields_doesNotThrow() throws IOException {
        // Arrange - only fullName set, everything else null
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("Minimal User");
        request.setEmail(null);
        request.setPhone(null);
        request.setLinkedinUrl(null);
        request.setSummary(null);
        request.setExperience(null);
        request.setEducation(null);
        request.setSkills(null);

        // Act & Assert - should not throw any exception
        byte[] result = assertDoesNotThrow(() -> cvBuilderService.generateCv(request));
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateCv_multipleExperienceEntries_returnsPdf() throws IOException {
        // Arrange
        CvBuilderRequest request = new CvBuilderRequest();
        request.setFullName("Multi Experience User");

        CvBuilderRequest.ExperienceEntry exp1 = new CvBuilderRequest.ExperienceEntry();
        exp1.setCompany("Company A");
        exp1.setRole("Junior Developer");
        exp1.setStartDate("2018-01");
        exp1.setEndDate("2020-06");
        exp1.setDescription("Worked on backend services.");

        CvBuilderRequest.ExperienceEntry exp2 = new CvBuilderRequest.ExperienceEntry();
        exp2.setCompany("Company B");
        exp2.setRole("Senior Developer");
        exp2.setStartDate("2020-07");
        exp2.setEndDate(null); // Current position - should show "Present"
        exp2.setDescription("Leading development of cloud infrastructure.");

        request.setExperience(List.of(exp1, exp2));

        // Act
        byte[] result = cvBuilderService.generateCv(request);

        // Assert
        assertNotNull(result);
        String header = new String(result, 0, 4);
        assertEquals("%PDF", header);
    }
}
