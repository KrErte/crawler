package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.SavedJob;
import ee.itjobs.entity.User;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.SavedJobRepository;
import ee.itjobs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private SavedJobService savedJobService;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("encoded")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private Job createTestJob(Long id, String title, String company) {
        return Job.builder()
                .id(id)
                .title(title)
                .company(company)
                .url("https://example.com/job/" + id)
                .source("cvkeskus")
                .build();
    }

    private JobDto createTestJobDto(Long id, String title, String company) {
        return JobDto.builder()
                .id(id)
                .title(title)
                .company(company)
                .url("https://example.com/job/" + id)
                .source("cvkeskus")
                .build();
    }

    // --- getSavedJobs ---

    @Test
    void getSavedJobs_emptyList_returnsEmptyList() {
        // Arrange
        String email = "user@example.com";
        when(savedJobRepository.findByUserEmailOrderBySavedAtDesc(email))
                .thenReturn(Collections.emptyList());

        // Act
        List<JobDto> result = savedJobService.getSavedJobs(email);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(savedJobRepository).findByUserEmailOrderBySavedAtDesc(email);
        verify(jobMapper, never()).toDto(any(Job.class));
    }

    @Test
    void getSavedJobs_withSavedJobs_returnsMappedDtos() {
        // Arrange
        String email = "user@example.com";
        User user = createTestUser();

        Job job1 = createTestJob(1L, "Java Developer", "CompanyA");
        Job job2 = createTestJob(2L, "Python Engineer", "CompanyB");

        SavedJob savedJob1 = SavedJob.builder()
                .id(100L)
                .user(user)
                .job(job1)
                .savedAt(LocalDateTime.now().minusHours(1))
                .build();

        SavedJob savedJob2 = SavedJob.builder()
                .id(101L)
                .user(user)
                .job(job2)
                .savedAt(LocalDateTime.now())
                .build();

        JobDto dto1 = createTestJobDto(1L, "Java Developer", "CompanyA");
        JobDto dto2 = createTestJobDto(2L, "Python Engineer", "CompanyB");

        when(savedJobRepository.findByUserEmailOrderBySavedAtDesc(email))
                .thenReturn(List.of(savedJob1, savedJob2));
        when(jobMapper.toDto(job1)).thenReturn(dto1);
        when(jobMapper.toDto(job2)).thenReturn(dto2);

        // Act
        List<JobDto> result = savedJobService.getSavedJobs(email);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        assertEquals("Python Engineer", result.get(1).getTitle());

        verify(savedJobRepository).findByUserEmailOrderBySavedAtDesc(email);
        verify(jobMapper).toDto(job1);
        verify(jobMapper).toDto(job2);
    }

    // --- saveJob ---

    @Test
    void saveJob_newSave_savesSuccessfully() {
        // Arrange
        String email = "user@example.com";
        Long jobId = 10L;
        User user = createTestUser();
        Job job = createTestJob(jobId, "React Developer", "TechCo");

        when(savedJobRepository.existsByUserEmailAndJobId(email, jobId)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(savedJobRepository.save(any(SavedJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        savedJobService.saveJob(email, jobId);

        // Assert
        verify(savedJobRepository).existsByUserEmailAndJobId(email, jobId);
        verify(userRepository).findByEmail(email);
        verify(jobRepository).findById(jobId);
        verify(savedJobRepository).save(any(SavedJob.class));
    }

    @Test
    void saveJob_alreadySaved_skipsIdempotently() {
        // Arrange
        String email = "user@example.com";
        Long jobId = 10L;

        when(savedJobRepository.existsByUserEmailAndJobId(email, jobId)).thenReturn(true);

        // Act
        savedJobService.saveJob(email, jobId);

        // Assert
        verify(savedJobRepository).existsByUserEmailAndJobId(email, jobId);
        verify(userRepository, never()).findByEmail(anyString());
        verify(jobRepository, never()).findById(anyLong());
        verify(savedJobRepository, never()).save(any(SavedJob.class));
    }

    @Test
    void saveJob_userNotFound_throwsException() {
        // Arrange
        String email = "nonexistent@example.com";
        Long jobId = 10L;

        when(savedJobRepository.existsByUserEmailAndJobId(email, jobId)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> savedJobService.saveJob(email, jobId)
        );
        assertEquals("User not found", exception.getMessage());

        verify(savedJobRepository).existsByUserEmailAndJobId(email, jobId);
        verify(userRepository).findByEmail(email);
        verify(jobRepository, never()).findById(anyLong());
        verify(savedJobRepository, never()).save(any(SavedJob.class));
    }

    @Test
    void saveJob_jobNotFound_throwsException() {
        // Arrange
        String email = "user@example.com";
        Long jobId = 999L;
        User user = createTestUser();

        when(savedJobRepository.existsByUserEmailAndJobId(email, jobId)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> savedJobService.saveJob(email, jobId)
        );
        assertEquals("Job not found", exception.getMessage());

        verify(savedJobRepository).existsByUserEmailAndJobId(email, jobId);
        verify(userRepository).findByEmail(email);
        verify(jobRepository).findById(jobId);
        verify(savedJobRepository, never()).save(any(SavedJob.class));
    }

    // --- unsaveJob ---

    @Test
    void unsaveJob_callsDeleteByUserEmailAndJobId() {
        // Arrange
        String email = "user@example.com";
        Long jobId = 10L;

        // Act
        savedJobService.unsaveJob(email, jobId);

        // Assert
        verify(savedJobRepository).deleteByUserEmailAndJobId(email, jobId);
    }

    // --- getSavedJobIds ---

    @Test
    void getSavedJobIds_returnsSetOfIds() {
        // Arrange
        String email = "user@example.com";
        when(savedJobRepository.findJobIdsByUserEmail(email))
                .thenReturn(List.of(1L, 5L, 12L));

        // Act
        Set<Long> result = savedJobService.getSavedJobIds(email);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(5L));
        assertTrue(result.contains(12L));

        verify(savedJobRepository).findJobIdsByUserEmail(email);
    }

    @Test
    void getSavedJobIds_noSavedJobs_returnsEmptySet() {
        // Arrange
        String email = "user@example.com";
        when(savedJobRepository.findJobIdsByUserEmail(email))
                .thenReturn(Collections.emptyList());

        // Act
        Set<Long> result = savedJobService.getSavedJobIds(email);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(savedJobRepository).findJobIdsByUserEmail(email);
    }
}
