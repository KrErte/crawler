package ee.itjobs.service;

import ee.itjobs.dto.application.ApplicationDto;
import ee.itjobs.dto.application.CreateApplicationRequest;
import ee.itjobs.entity.Application;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.User;
import ee.itjobs.enums.ApplicationStatus;
import ee.itjobs.exception.DuplicateResourceException;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.ApplicationMapper;
import ee.itjobs.repository.ApplicationRepository;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private ApplicationService applicationService;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("encoded")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private Job createTestJob() {
        return Job.builder()
                .id(10L)
                .title("Java Developer")
                .company("TestCo")
                .url("https://example.com/job/10")
                .source("cvkeskus")
                .build();
    }

    @Test
    void createApplication_success() {
        // Arrange
        User user = createTestUser();
        Job job = createTestJob();

        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setJobId(10L);
        request.setNotes("Interested in this role");

        Application savedApp = Application.builder()
                .id(100L)
                .user(user)
                .job(job)
                .notes("Interested in this role")
                .status(ApplicationStatus.SUBMITTED)
                .appliedAt(LocalDateTime.now())
                .build();

        ApplicationDto expectedDto = ApplicationDto.builder()
                .id(100L)
                .jobId(10L)
                .jobTitle("Java Developer")
                .company("TestCo")
                .jobUrl("https://example.com/job/10")
                .source("cvkeskus")
                .status(ApplicationStatus.SUBMITTED)
                .notes("Interested in this role")
                .appliedAt(savedApp.getAppliedAt())
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(savedApp);
        when(applicationMapper.toDto(savedApp)).thenReturn(expectedDto);

        // Act
        ApplicationDto result = applicationService.createApplication("user@example.com", request);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(10L, result.getJobId());
        assertEquals("Java Developer", result.getJobTitle());
        assertEquals(ApplicationStatus.SUBMITTED, result.getStatus());

        verify(userRepository).findByEmail("user@example.com");
        verify(jobRepository).findById(10L);
        verify(applicationRepository).existsByUserIdAndJobId(1L, 10L);
        verify(applicationRepository).save(any(Application.class));
        verify(applicationMapper).toDto(savedApp);
    }

    @Test
    void createApplication_duplicate_throwsException() {
        // Arrange
        User user = createTestUser();
        Job job = createTestJob();

        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setJobId(10L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> applicationService.createApplication("user@example.com", request)
        );
        assertEquals("Already applied to this job", exception.getMessage());

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void deleteApplication_success() {
        // Arrange
        User user = createTestUser();
        Job job = createTestJob();

        Application app = Application.builder()
                .id(100L)
                .user(user)
                .job(job)
                .status(ApplicationStatus.SUBMITTED)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));

        // Act
        applicationService.deleteApplication("user@example.com", 100L);

        // Assert
        verify(applicationRepository).findById(100L);
        verify(applicationRepository).delete(app);
    }

    @Test
    void deleteApplication_notOwner_throwsException() {
        // Arrange
        User user = createTestUser();
        User otherUser = User.builder().id(2L).email("other@example.com").build();

        Application app = Application.builder()
                .id(100L)
                .user(otherUser)
                .job(createTestJob())
                .status(ApplicationStatus.SUBMITTED)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.deleteApplication("user@example.com", 100L));

        verify(applicationRepository, never()).delete(any(Application.class));
    }

    @Test
    void exportToCsv_returnsBytes() {
        // Arrange
        User user = createTestUser();

        ApplicationDto dto = ApplicationDto.builder()
                .id(1L)
                .jobId(10L)
                .jobTitle("Java Developer")
                .company("TestCo")
                .jobUrl("https://example.com/job/10")
                .source("cvkeskus")
                .status(ApplicationStatus.SUBMITTED)
                .notes("Test notes")
                .appliedAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L))
                .thenReturn(List.of());
        when(applicationMapper.toDtoList(any())).thenReturn(List.of(dto));

        // Act
        byte[] csvBytes = applicationService.exportToCsv("user@example.com");

        // Assert
        assertNotNull(csvBytes);
        String csv = new String(csvBytes);
        assertTrue(csv.startsWith("Job Title,Company,Source,Status,Applied At,Notes,Job URL"));
        assertTrue(csv.contains("Java Developer"));
        assertTrue(csv.contains("TestCo"));
        assertTrue(csv.contains("SUBMITTED"));
    }
}
