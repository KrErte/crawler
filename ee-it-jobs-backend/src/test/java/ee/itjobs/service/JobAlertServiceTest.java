package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.match.MatchResultDto;
import ee.itjobs.entity.User;
import ee.itjobs.entity.UserProfile;
import ee.itjobs.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobAlertServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private MatchService matchService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private JobAlertService jobAlertService;

    private User createTestUser(String email) {
        return User.builder()
                .id(1L)
                .email(email)
                .passwordHash("encoded")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private UserProfile createAlertProfile(User user, String cvText, Integer threshold) {
        return UserProfile.builder()
                .id(10L)
                .user(user)
                .cvRawText(cvText)
                .emailAlerts(true)
                .alertThreshold(threshold)
                .build();
    }

    private MatchResultDto createMatch(String title, String company, int percentage) {
        JobDto jobDto = JobDto.builder()
                .id(1L)
                .title(title)
                .company(company)
                .url("https://example.com/job/" + title.hashCode())
                .source("cvkeskus")
                .build();

        return MatchResultDto.builder()
                .job(jobDto)
                .matchPercentage(percentage)
                .matchedSkills(List.of("java", "spring"))
                .matchExplanation("Good match for " + title)
                .build();
    }

    // --- sendDailyAlerts: successful alert delivery ---

    @Test
    void sendDailyAlerts_withMatchingUsers_sendsEmails() {
        // Arrange
        User user = createTestUser("dev@example.com");
        UserProfile profile = createAlertProfile(user, "Experienced Java developer with 5 years...", 70);

        MatchResultDto match1 = createMatch("Java Developer", "CompanyA", 85);
        MatchResultDto match2 = createMatch("Spring Engineer", "CompanyB", 78);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));
        when(matchService.matchJobsFromProfile("dev@example.com", 10))
                .thenReturn(List.of(match1, match2));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(userProfileRepository).findByEmailAlertsTrue();
        verify(matchService).matchJobsFromProfile("dev@example.com", 10);
        verify(emailService).sendJobAlertEmail(
                eq("dev@example.com"),
                eq("EE IT Jobs - 2 new matches for you!"),
                anyString());
        verify(userProfileRepository).save(profile);
    }

    @Test
    void sendDailyAlerts_multipleProfiles_sendsToAll() {
        // Arrange
        User user1 = createTestUser("dev1@example.com");
        User user2 = User.builder().id(2L).email("dev2@example.com").passwordHash("encoded").build();

        UserProfile profile1 = createAlertProfile(user1, "Java developer CV text", 70);
        UserProfile profile2 = UserProfile.builder()
                .id(20L)
                .user(user2)
                .cvRawText("Python developer CV text")
                .emailAlerts(true)
                .alertThreshold(60)
                .build();

        MatchResultDto match1 = createMatch("Java Developer", "CompanyA", 80);
        MatchResultDto match2 = createMatch("Python Engineer", "CompanyB", 75);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile1, profile2));
        when(matchService.matchJobsFromProfile("dev1@example.com", 10)).thenReturn(List.of(match1));
        when(matchService.matchJobsFromProfile("dev2@example.com", 10)).thenReturn(List.of(match2));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(emailService).sendJobAlertEmail(eq("dev1@example.com"), anyString(), anyString());
        verify(emailService).sendJobAlertEmail(eq("dev2@example.com"), anyString(), anyString());
        verify(userProfileRepository, times(2)).save(any(UserProfile.class));
    }

    // --- sendDailyAlerts: profiles without CV ---

    @Test
    void sendDailyAlerts_profileWithNullCv_skipsProfile() {
        // Arrange
        User user = createTestUser("nocv@example.com");
        UserProfile profile = createAlertProfile(user, null, 70);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(userProfileRepository).findByEmailAlertsTrue();
        verify(matchService, never()).matchJobsFromProfile(anyString(), anyInt());
        verify(emailService, never()).sendJobAlertEmail(anyString(), anyString(), anyString());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void sendDailyAlerts_profileWithBlankCv_skipsProfile() {
        // Arrange
        User user = createTestUser("blankcv@example.com");
        UserProfile profile = createAlertProfile(user, "   ", 70);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(matchService, never()).matchJobsFromProfile(anyString(), anyInt());
        verify(emailService, never()).sendJobAlertEmail(anyString(), anyString(), anyString());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    // --- sendDailyAlerts: threshold filtering ---

    @Test
    void sendDailyAlerts_noMatchesAboveThreshold_doesNotSendEmail() {
        // Arrange
        User user = createTestUser("dev@example.com");
        UserProfile profile = createAlertProfile(user, "Senior Java developer with Spring...", 80);

        MatchResultDto lowMatch1 = createMatch("Junior PHP Dev", "CompanyA", 45);
        MatchResultDto lowMatch2 = createMatch("Python Intern", "CompanyB", 55);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));
        when(matchService.matchJobsFromProfile("dev@example.com", 10))
                .thenReturn(List.of(lowMatch1, lowMatch2));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(matchService).matchJobsFromProfile("dev@example.com", 10);
        verify(emailService, never()).sendJobAlertEmail(anyString(), anyString(), anyString());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void sendDailyAlerts_defaultThreshold_usesSeventyPercent() {
        // Arrange
        User user = createTestUser("dev@example.com");
        UserProfile profile = createAlertProfile(user, "Java developer CV", null);

        MatchResultDto aboveDefault = createMatch("Java Developer", "CompanyA", 72);
        MatchResultDto belowDefault = createMatch("Marketing Analyst", "CompanyB", 65);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));
        when(matchService.matchJobsFromProfile("dev@example.com", 10))
                .thenReturn(List.of(aboveDefault, belowDefault));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(emailService).sendJobAlertEmail(
                eq("dev@example.com"),
                eq("EE IT Jobs - 1 new matches for you!"),
                anyString());
        verify(userProfileRepository).save(profile);
    }

    @Test
    void sendDailyAlerts_customThreshold_filtersCorrectly() {
        // Arrange
        User user = createTestUser("dev@example.com");
        UserProfile profile = createAlertProfile(user, "Python expert CV", 90);

        MatchResultDto match85 = createMatch("Python Dev", "CompanyA", 85);
        MatchResultDto match92 = createMatch("Senior Python", "CompanyB", 92);
        MatchResultDto match95 = createMatch("Lead Python Eng", "CompanyC", 95);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));
        when(matchService.matchJobsFromProfile("dev@example.com", 10))
                .thenReturn(List.of(match85, match92, match95));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(emailService).sendJobAlertEmail(
                eq("dev@example.com"),
                eq("EE IT Jobs - 2 new matches for you!"),
                anyString());
        verify(userProfileRepository).save(profile);
    }

    // --- sendDailyAlerts: error handling ---

    @Test
    void sendDailyAlerts_matchServiceThrows_continuesWithOtherProfiles() {
        // Arrange
        User user1 = createTestUser("failing@example.com");
        User user2 = User.builder().id(2L).email("working@example.com").passwordHash("encoded").build();

        UserProfile failingProfile = createAlertProfile(user1, "Some CV text", 70);
        UserProfile workingProfile = UserProfile.builder()
                .id(20L)
                .user(user2)
                .cvRawText("Another CV text")
                .emailAlerts(true)
                .alertThreshold(70)
                .build();

        MatchResultDto goodMatch = createMatch("Java Dev", "CompanyB", 80);

        when(userProfileRepository.findByEmailAlertsTrue())
                .thenReturn(List.of(failingProfile, workingProfile));
        when(matchService.matchJobsFromProfile("failing@example.com", 10))
                .thenThrow(new RuntimeException("Match service error"));
        when(matchService.matchJobsFromProfile("working@example.com", 10))
                .thenReturn(List.of(goodMatch));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert - first profile fails but second still gets processed
        verify(emailService, never()).sendJobAlertEmail(eq("failing@example.com"), anyString(), anyString());
        verify(emailService).sendJobAlertEmail(eq("working@example.com"), anyString(), anyString());
        verify(userProfileRepository).save(workingProfile);
    }

    // --- sendDailyAlerts: empty matches ---

    @Test
    void sendDailyAlerts_emptyMatchList_doesNotSendEmail() {
        // Arrange
        User user = createTestUser("dev@example.com");
        UserProfile profile = createAlertProfile(user, "Developer with Python skills", 70);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));
        when(matchService.matchJobsFromProfile("dev@example.com", 10))
                .thenReturn(Collections.emptyList());

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(matchService).matchJobsFromProfile("dev@example.com", 10);
        verify(emailService, never()).sendJobAlertEmail(anyString(), anyString(), anyString());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void sendDailyAlerts_noAlertProfiles_doesNothing() {
        // Arrange
        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(Collections.emptyList());

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        verify(userProfileRepository).findByEmailAlertsTrue();
        verify(matchService, never()).matchJobsFromProfile(anyString(), anyInt());
        verify(emailService, never()).sendJobAlertEmail(anyString(), anyString(), anyString());
    }

    // --- sendDailyAlerts: updatesLastAlertSentAt ---

    @Test
    void sendDailyAlerts_successfulSend_updatesLastAlertSentAt() {
        // Arrange
        User user = createTestUser("dev@example.com");
        UserProfile profile = createAlertProfile(user, "Java developer CV", 70);
        assertNull(profile.getLastAlertSentAt());

        MatchResultDto match = createMatch("Java Dev", "CompanyA", 85);

        when(userProfileRepository.findByEmailAlertsTrue()).thenReturn(List.of(profile));
        when(matchService.matchJobsFromProfile("dev@example.com", 10))
                .thenReturn(List.of(match));

        // Act
        jobAlertService.sendDailyAlerts();

        // Assert
        assertNotNull(profile.getLastAlertSentAt());
        verify(userProfileRepository).save(profile);
    }
}
