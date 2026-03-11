package ee.itjobs.service;

import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserProfileRepository;
import ee.itjobs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    void extractProfile_withSkills_extractsKnownSkills() {
        // Arrange
        String cvText = "Experienced software developer with 5 years of experience. " +
                "Skills: Java, Python, Spring Boot, Docker, Kubernetes, AWS, PostgreSQL. " +
                "Familiar with React and TypeScript for frontend development.";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile);
        assertNotNull(profile.skills());
        assertTrue(profile.skills().contains("java"));
        assertTrue(profile.skills().contains("python"));
        assertTrue(profile.skills().contains("spring"));
        assertTrue(profile.skills().contains("docker"));
        assertTrue(profile.skills().contains("kubernetes"));
        assertTrue(profile.skills().contains("aws"));
        assertTrue(profile.skills().contains("postgresql"));
        assertTrue(profile.skills().contains("react"));
        assertTrue(profile.skills().contains("typescript"));
    }

    @Test
    void extractProfile_withYearsExperience_detectsSeniorLevel() {
        // Arrange
        String cvText = "Senior developer with 8 years of experience in Java and Spring.";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile);
        assertEquals(8, profile.yearsExperience());
        assertEquals("senior", profile.roleLevel());
    }

    @Test
    void extractProfile_withFewYears_detectsJuniorLevel() {
        // Arrange
        String cvText = "Developer with 1 year of experience in Python and Django.";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile);
        assertEquals(1, profile.yearsExperience());
        assertEquals("junior", profile.roleLevel());
    }

    @Test
    void extractProfile_withMidExperience_detectsMidLevel() {
        // Arrange
        String cvText = "Full-stack developer with 3 years of experience in React and Node.js.";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile);
        assertEquals(3, profile.yearsExperience());
        assertEquals("mid", profile.roleLevel());
    }

    @Test
    void extractProfile_noExperienceButSeniorKeyword_detectsSenior() {
        // Arrange - no "X years" pattern, but "senior" keyword present
        String cvText = "Senior software architect proficient in Java and microservices.";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile);
        assertNull(profile.yearsExperience());
        assertEquals("senior", profile.roleLevel());
    }

    @Test
    void extractProfile_emptyText_returnsEmptyProfile() {
        // Arrange
        String cvText = "";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile);
        assertTrue(profile.skills().isEmpty());
        assertNull(profile.yearsExperience());
        assertNull(profile.roleLevel());
    }

    @Test
    void extractProfile_collectsAllKeywords() {
        // Arrange
        String cvText = "Experienced developer with Python and Docker skills.";

        // Act
        MatchService.CVProfile profile = matchService.extractProfile(cvText);

        // Assert
        assertNotNull(profile.allKeywords());
        // allKeywords should contain the extracted skills plus words from the text
        assertTrue(profile.allKeywords().contains("python"));
        assertTrue(profile.allKeywords().contains("docker"));
        assertTrue(profile.allKeywords().contains("experienced"));
        assertTrue(profile.allKeywords().contains("developer"));
    }
}
