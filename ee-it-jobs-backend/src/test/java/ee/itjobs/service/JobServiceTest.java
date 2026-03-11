package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.job.JobFilterDto;
import ee.itjobs.entity.Job;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    @Test
    void isItRelated_itTitle_returnsTrue() {
        // "Software Engineer" contains "software" which is an IT keyword
        assertTrue(JobService.isItRelated("Software Engineer", null));
        assertTrue(JobService.isItRelated("Senior Java Developer", null));
        assertTrue(JobService.isItRelated("DevOps Engineer", null));
        assertTrue(JobService.isItRelated("Frontend Developer", null));
        assertTrue(JobService.isItRelated("Tarkvara Arendaja", null));
    }

    @Test
    void isItRelated_nonItTitle_returnsFalse() {
        assertFalse(JobService.isItRelated("Marketing Manager", null));
        assertFalse(JobService.isItRelated("Accountant", null));
        assertFalse(JobService.isItRelated("Sales Representative", null));
        assertFalse(JobService.isItRelated(null, null));
    }

    @Test
    void isItRelated_itDepartment_returnsTrue() {
        // Title alone is not IT, but department contains IT keyword
        assertTrue(JobService.isItRelated("Team Lead", "Software Development"));
        assertTrue(JobService.isItRelated("Manager", "Infrastructure"));
    }

    @Test
    void getJob_existingId_returnsDto() {
        // Arrange
        Long jobId = 1L;
        Job job = Job.builder()
                .id(jobId)
                .title("Java Developer")
                .company("TestCo")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .build();

        JobDto expectedDto = JobDto.builder()
                .id(jobId)
                .title("Java Developer")
                .company("TestCo")
                .url("https://example.com/job/1")
                .source("cvkeskus")
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMapper.toDto(job)).thenReturn(expectedDto);

        // Act
        JobDto result = jobService.getJob(jobId);

        // Assert
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertEquals("Java Developer", result.getTitle());
        assertEquals("TestCo", result.getCompany());

        verify(jobRepository).findById(jobId);
        verify(jobMapper).toDto(job);
    }

    @Test
    void getJob_nonExistingId_throwsException() {
        // Arrange
        Long jobId = 999L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.getJob(jobId)
        );
        assertEquals("Job not found", exception.getMessage());

        verify(jobRepository).findById(jobId);
        verify(jobMapper, never()).toDto(any(Job.class));
    }

    @Test
    void getFilters_returnsFilters() {
        // Arrange
        List<String> companies = List.of("CompanyA", "CompanyB");
        List<String> sources = List.of("cvkeskus", "cv.ee");

        when(jobRepository.findDistinctCompanies()).thenReturn(companies);
        when(jobRepository.findDistinctSources()).thenReturn(sources);

        // Act
        JobFilterDto result = jobService.getFilters();

        // Assert
        assertNotNull(result);
        assertEquals(companies, result.getCompanies());
        assertEquals(sources, result.getSources());
        assertNotNull(result.getJobTypes());
        assertFalse(result.getJobTypes().isEmpty());
        assertNotNull(result.getWorkplaceTypes());
        assertFalse(result.getWorkplaceTypes().isEmpty());

        verify(jobRepository).findDistinctCompanies();
        verify(jobRepository).findDistinctSources();
    }
}
