package ee.itjobs.service;

import ee.itjobs.entity.Job;
import ee.itjobs.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private DeduplicationService deduplicationService;

    @Test
    void upsertJob_newJob_returnsTrue() {
        // Arrange
        Job job = Job.builder()
                .title("Java Developer")
                .company("TestCo OUe")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .dedupKey("cvkeskus:java-developer-testco")
                .dateScraped(LocalDate.now())
                .build();

        when(jobRepository.findByDedupKey(job.getDedupKey())).thenReturn(Optional.empty());
        when(jobRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        // Act
        boolean isNew = deduplicationService.upsertJob(job);

        // Assert
        assertTrue(isNew);
        verify(jobRepository).findByDedupKey(job.getDedupKey());
        verify(jobRepository).findByIsActiveTrue();
        verify(jobRepository).save(job);
    }

    @Test
    void upsertJob_existingJob_returnsFalse() {
        // Arrange
        Job existingJob = Job.builder()
                .id(1L)
                .title("Java Developer")
                .company("TestCo")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .dedupKey("cvkeskus:java-developer-testco")
                .dateScraped(LocalDate.now().minusDays(1))
                .isActive(true)
                .build();

        Job newJob = Job.builder()
                .title("Java Developer")
                .company("TestCo")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .dedupKey("cvkeskus:java-developer-testco")
                .dateScraped(LocalDate.now())
                .build();

        when(jobRepository.findByDedupKey(newJob.getDedupKey())).thenReturn(Optional.of(existingJob));
        when(jobRepository.save(any(Job.class))).thenReturn(existingJob);

        // Act
        boolean isNew = deduplicationService.upsertJob(newJob);

        // Assert
        assertFalse(isNew);
        verify(jobRepository).findByDedupKey(newJob.getDedupKey());
        verify(jobRepository).save(existingJob);
        assertEquals(LocalDate.now(), existingJob.getDateScraped());
        assertTrue(existingJob.getIsActive());
    }

    @Test
    void upsertJob_fuzzyMatch_returnsFalse() {
        // Arrange - job with slightly different title but same company
        Job existingJob = Job.builder()
                .id(1L)
                .title("Senior Java Developer")
                .company("TestCo AS")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .dedupKey("cvkeskus:senior-java-dev-testco")
                .dateScraped(LocalDate.now().minusDays(1))
                .isActive(true)
                .build();

        Job newJob = Job.builder()
                .title("Senior Java Developer")
                .company("TestCo OUe")
                .url("https://example.com/job/2")
                .source("cv.ee")
                .dedupKey("cv.ee:senior-java-developer-testco")
                .dateScraped(LocalDate.now())
                .build();

        when(jobRepository.findByDedupKey(newJob.getDedupKey())).thenReturn(Optional.empty());
        when(jobRepository.findByIsActiveTrue()).thenReturn(List.of(existingJob));
        when(jobRepository.save(any(Job.class))).thenReturn(existingJob);

        // Act
        boolean isNew = deduplicationService.upsertJob(newJob);

        // Assert
        assertFalse(isNew);
        verify(jobRepository).findByDedupKey(newJob.getDedupKey());
        verify(jobRepository).findByIsActiveTrue();
        verify(jobRepository).save(existingJob);
    }

    @Test
    void extractSkills_detectsKnownSkills() {
        // Arrange
        Job job = Job.builder()
                .title("Full Stack Developer")
                .descriptionSnippet("We are looking for a developer skilled in Java, React, and Docker.")
                .fullDescription("Experience with PostgreSQL and AWS required. CI/CD pipeline management.")
                .build();

        // Act
        List<String> skills = DeduplicationService.extractSkills(job);

        // Assert
        assertNotNull(skills);
        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("React"));
        assertTrue(skills.contains("Docker"));
        assertTrue(skills.contains("PostgreSQL"));
        assertTrue(skills.contains("AWS"));
        assertTrue(skills.contains("CI/CD"));
    }

    @Test
    void extractSkills_noSkills_returnsEmptyList() {
        // Arrange
        Job job = Job.builder()
                .title("Office Manager")
                .descriptionSnippet("We need someone to manage the office.")
                .fullDescription(null)
                .build();

        // Act
        List<String> skills = DeduplicationService.extractSkills(job);

        // Assert
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }
}
