package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.entity.Application;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.User;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.ApplicationRepository;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void getRecommendations_noApplications_returnsEmpty() {
        User user = User.builder().id(1L).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L)).thenReturn(List.of());

        List<JobDto> result = recommendationService.getRecommendations("user@test.com", 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRecommendations_userNotFound_throws() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> recommendationService.getRecommendations("missing@test.com", 10));
    }

    @Test
    void getRecommendations_companyScoringWorks() {
        User user = User.builder().id(1L).email("user@test.com").build();
        Job appliedJob = Job.builder().id(10L).title("Java Developer").company("Wise")
                .skills(List.of("Java")).build();
        Application app = Application.builder().user(user).job(appliedJob).build();

        Job candidate1 = Job.builder().id(20L).title("Python Developer").company("Wise")
                .isActive(true).skills(List.of("Python")).build();
        Job candidate2 = Job.builder().id(21L).title("Go Developer").company("Bolt")
                .isActive(true).skills(List.of("Go")).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L)).thenReturn(List.of(app));
        when(jobRepository.findByIsActiveTrueOrderByDateScrapedDesc()).thenReturn(List.of(candidate1, candidate2));
        when(jobMapper.toDto(candidate1)).thenReturn(JobDto.builder().id(20L).company("Wise").build());

        List<JobDto> result = recommendationService.getRecommendations("user@test.com", 10);

        // candidate1 (Wise) should score higher due to company match
        assertFalse(result.isEmpty());
        assertEquals("Wise", result.get(0).getCompany());
    }

    @Test
    void getRecommendations_skillOverlapScores() {
        User user = User.builder().id(1L).email("user@test.com").build();
        Job appliedJob = Job.builder().id(10L).title("Developer").company("A")
                .skills(List.of("Java", "Spring")).build();
        Application app = Application.builder().user(user).job(appliedJob).build();

        Job candidate = Job.builder().id(20L).title("Engineer").company("B")
                .isActive(true).skills(List.of("Java", "Spring", "Docker")).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L)).thenReturn(List.of(app));
        when(jobRepository.findByIsActiveTrueOrderByDateScrapedDesc()).thenReturn(List.of(candidate));
        when(jobMapper.toDto(candidate)).thenReturn(JobDto.builder().id(20L).build());

        List<JobDto> result = recommendationService.getRecommendations("user@test.com", 10);

        assertFalse(result.isEmpty());
    }

    @Test
    void getRecommendations_titleKeywordScores() {
        User user = User.builder().id(1L).email("user@test.com").build();
        Job appliedJob = Job.builder().id(10L).title("Senior Java Developer").company("A")
                .skills(List.of()).build();
        Application app = Application.builder().user(user).job(appliedJob).build();

        Job candidate = Job.builder().id(20L).title("Junior Java Developer").company("B")
                .isActive(true).skills(List.of()).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L)).thenReturn(List.of(app));
        when(jobRepository.findByIsActiveTrueOrderByDateScrapedDesc()).thenReturn(List.of(candidate));
        when(jobMapper.toDto(candidate)).thenReturn(JobDto.builder().id(20L).build());

        List<JobDto> result = recommendationService.getRecommendations("user@test.com", 10);

        // "developer" and "java" are >3 chars, so they match
        assertFalse(result.isEmpty());
    }

    @Test
    void getRecommendations_excludesAlreadyApplied() {
        User user = User.builder().id(1L).email("user@test.com").build();
        Job appliedJob = Job.builder().id(10L).title("Java Developer").company("Wise")
                .skills(List.of("Java")).build();
        Application app = Application.builder().user(user).job(appliedJob).build();

        // Same job ID = already applied
        Job sameJob = Job.builder().id(10L).title("Java Developer").company("Wise")
                .isActive(true).skills(List.of("Java")).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L)).thenReturn(List.of(app));
        when(jobRepository.findByIsActiveTrueOrderByDateScrapedDesc()).thenReturn(List.of(sameJob));

        List<JobDto> result = recommendationService.getRecommendations("user@test.com", 10);

        assertTrue(result.isEmpty());
        verify(jobMapper, never()).toDto(any());
    }

    @Test
    void getRecommendations_respectsLimit() {
        User user = User.builder().id(1L).email("user@test.com").build();
        Job appliedJob = Job.builder().id(10L).title("Java Developer").company("Wise")
                .skills(List.of("Java")).build();
        Application app = Application.builder().user(user).job(appliedJob).build();

        Job c1 = Job.builder().id(20L).title("Java Senior").company("B").isActive(true).skills(List.of("Java")).build();
        Job c2 = Job.builder().id(21L).title("Java Junior").company("C").isActive(true).skills(List.of("Java")).build();
        Job c3 = Job.builder().id(22L).title("Java Mid").company("D").isActive(true).skills(List.of("Java")).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(1L)).thenReturn(List.of(app));
        when(jobRepository.findByIsActiveTrueOrderByDateScrapedDesc()).thenReturn(List.of(c1, c2, c3));
        when(jobMapper.toDto(any(Job.class))).thenReturn(JobDto.builder().id(1L).build());

        List<JobDto> result = recommendationService.getRecommendations("user@test.com", 2);

        assertEquals(2, result.size());
    }
}
