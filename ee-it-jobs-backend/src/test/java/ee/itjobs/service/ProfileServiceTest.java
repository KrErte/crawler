package ee.itjobs.service;

import ee.itjobs.dto.profile.ProfileDto;
import ee.itjobs.dto.profile.ProfileUpdateRequest;
import ee.itjobs.entity.User;
import ee.itjobs.entity.UserProfile;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.repository.UserProfileRepository;
import ee.itjobs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private ProfileService profileService;

    // -------------------------------------------------------------------------
    // getProfile
    // -------------------------------------------------------------------------

    @Test
    void getProfile_withExistingProfile_returnsFullDto() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .phone("+37255512345")
                .linkedinUrl("https://linkedin.com/in/johndoe")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .coverLetter("I am a skilled developer.")
                .cvFilePath("/storage/cv_1_abc.pdf")
                .skills(List.of("java", "spring", "python"))
                .preferences(Map.of("remote", true))
                .cvRawText("John Doe - Software Engineer...")
                .yearsExperience(5)
                .roleLevel("mid")
                .emailAlerts(true)
                .alertThreshold(80)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        // Act
        ProfileDto result = profileService.getProfile("john@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("+37255512345", result.getPhone());
        assertEquals("https://linkedin.com/in/johndoe", result.getLinkedinUrl());
        assertEquals("I am a skilled developer.", result.getCoverLetter());
        assertEquals(List.of("java", "spring", "python"), result.getSkills());
        assertEquals(Map.of("remote", true), result.getPreferences());
        assertEquals("John Doe - Software Engineer...", result.getCvRawText());
        assertEquals(5, result.getYearsExperience());
        assertEquals("mid", result.getRoleLevel());
        assertTrue(result.isHasCv());
        assertTrue(result.getEmailAlerts());
        assertEquals(80, result.getAlertThreshold());

        verify(userRepository).findByEmail("john@example.com");
        verify(profileRepository).findByUserId(1L);
    }

    @Test
    void getProfile_withoutProfile_returnsPartialDto() {
        // Arrange
        User user = User.builder()
                .id(2L)
                .email("jane@example.com")
                .passwordHash("hashed")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.empty());

        // Act
        ProfileDto result = profileService.getProfile("jane@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals("jane@example.com", result.getEmail());
        assertNull(result.getCoverLetter());
        assertNull(result.getSkills());
        assertNull(result.getPreferences());
        assertNull(result.getCvRawText());
        assertNull(result.getYearsExperience());
        assertNull(result.getRoleLevel());
        assertFalse(result.isHasCv());
        assertNull(result.getEmailAlerts());
        assertNull(result.getAlertThreshold());
    }

    @Test
    void getProfile_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.getProfile("ghost@example.com")
        );
        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findByEmail("ghost@example.com");
        verify(profileRepository, never()).findByUserId(any());
    }

    // -------------------------------------------------------------------------
    // updateProfile
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_existingProfile_updatesAllFields() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("OldFirst")
                .lastName("OldLast")
                .build();

        UserProfile existingProfile = UserProfile.builder()
                .id(10L)
                .user(user)
                .coverLetter("Old cover letter")
                .emailAlerts(false)
                .alertThreshold(70)
                .build();

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("+37255599999");
        request.setLinkedinUrl("https://linkedin.com/in/johndoe");
        request.setCoverLetter("Updated cover letter");
        request.setEmailAlerts(true);
        request.setAlertThreshold(85);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(existingProfile);

        // Act
        ProfileDto result = profileService.updateProfile("john@example.com", request);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("+37255599999", result.getPhone());
        assertEquals("https://linkedin.com/in/johndoe", result.getLinkedinUrl());
        assertEquals("Updated cover letter", result.getCoverLetter());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("John", userCaptor.getValue().getFirstName());
        assertEquals("Doe", userCaptor.getValue().getLastName());
        assertEquals("+37255599999", userCaptor.getValue().getPhone());
        assertEquals("https://linkedin.com/in/johndoe", userCaptor.getValue().getLinkedinUrl());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertEquals("Updated cover letter", profileCaptor.getValue().getCoverLetter());
        assertTrue(profileCaptor.getValue().getEmailAlerts());
        assertEquals(85, profileCaptor.getValue().getAlertThreshold());
    }

    @Test
    void updateProfile_noExistingProfile_createsNewProfile() {
        // Arrange
        User user = User.builder()
                .id(3L)
                .email("new@example.com")
                .passwordHash("hashed")
                .build();

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhone(null);
        request.setLinkedinUrl(null);
        request.setCoverLetter("My first cover letter");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(profileRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.updateProfile("new@example.com", request);

        // Assert
        assertNotNull(result);
        assertEquals("My first cover letter", result.getCoverLetter());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();
        assertEquals(user, savedProfile.getUser());
        assertEquals("My first cover letter", savedProfile.getCoverLetter());
    }

    @Test
    void updateProfile_emailAlertsNull_doesNotOverwrite() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        UserProfile existingProfile = UserProfile.builder()
                .id(10L)
                .user(user)
                .emailAlerts(true)
                .alertThreshold(90)
                .build();

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setCoverLetter("Updated");
        // emailAlerts and alertThreshold are null in request

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(existingProfile);

        // Act
        profileService.updateProfile("john@example.com", request);

        // Assert
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertTrue(profileCaptor.getValue().getEmailAlerts());
        assertEquals(90, profileCaptor.getValue().getAlertThreshold());
    }

    @Test
    void updateProfile_setEmailAlertsAndThreshold() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        UserProfile existingProfile = UserProfile.builder()
                .id(10L)
                .user(user)
                .emailAlerts(false)
                .alertThreshold(70)
                .build();

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setCoverLetter("Cover");
        request.setEmailAlerts(true);
        request.setAlertThreshold(60);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(existingProfile);

        // Act
        profileService.updateProfile("john@example.com", request);

        // Assert
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertTrue(profileCaptor.getValue().getEmailAlerts());
        assertEquals(60, profileCaptor.getValue().getAlertThreshold());
    }

    // -------------------------------------------------------------------------
    // uploadCv
    // -------------------------------------------------------------------------

    @Test
    void uploadCv_newCv_storesAndExtractsSkills() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "fake-pdf-content".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "John Doe Java Developer 5 years experience",
                Set.of("java", "spring", "sql"),
                5,
                "mid",
                Set.of("java", "spring", "sql", "developer")
        );

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(fileStorageService.storeCv(mockFile, 1L)).thenReturn("/storage/cv_1_uuid.pdf");
        when(matchService.extractPdfText(fileBytes)).thenReturn("John Doe Java Developer 5 years experience");
        when(matchService.extractProfile("John Doe Java Developer 5 years experience")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.uploadCv("john@example.com", mockFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.isHasCv());
        assertEquals("John Doe Java Developer 5 years experience", result.getCvRawText());
        assertEquals(5, result.getYearsExperience());
        assertEquals("mid", result.getRoleLevel());
        assertNotNull(result.getSkills());
        assertTrue(result.getSkills().containsAll(List.of("java", "spring", "sql")));

        verify(fileStorageService).storeCv(mockFile, 1L);
        verify(fileStorageService, never()).deleteCv(any());
        verify(matchService).extractPdfText(fileBytes);
        verify(matchService).extractProfile("John Doe Java Developer 5 years experience");

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertEquals("/storage/cv_1_uuid.pdf", profileCaptor.getValue().getCvFilePath());
    }

    @Test
    void uploadCv_replaceExistingCv_deletesOldAndStoresNew() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .cvFilePath("/storage/old_cv.pdf")
                .cvRawText("Old text")
                .skills(List.of("python"))
                .yearsExperience(3)
                .roleLevel("junior")
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "new-pdf-content".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "Updated CV text",
                Set.of("java", "spring"),
                7,
                "senior",
                Set.of("java", "spring", "senior")
        );

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(fileStorageService.storeCv(mockFile, 1L)).thenReturn("/storage/cv_1_newuuid.pdf");
        when(matchService.extractPdfText(fileBytes)).thenReturn("Updated CV text");
        when(matchService.extractProfile("Updated CV text")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.uploadCv("john@example.com", mockFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.isHasCv());
        assertEquals("Updated CV text", result.getCvRawText());
        assertEquals(7, result.getYearsExperience());
        assertEquals("senior", result.getRoleLevel());
        assertTrue(result.getSkills().containsAll(List.of("java", "spring")));

        verify(fileStorageService).deleteCv("/storage/old_cv.pdf");
        verify(fileStorageService).storeCv(mockFile, 1L);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertEquals("/storage/cv_1_newuuid.pdf", profileCaptor.getValue().getCvFilePath());
    }

    @Test
    void uploadCv_noExistingProfile_createsNewProfile() throws IOException {
        // Arrange
        User user = User.builder()
                .id(5L)
                .email("fresh@example.com")
                .passwordHash("hashed")
                .firstName("Fresh")
                .lastName("User")
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "pdf-bytes".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "Fresh user CV text",
                Set.of("react", "typescript"),
                2,
                "junior",
                Set.of("react", "typescript", "junior")
        );

        when(userRepository.findByEmail("fresh@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(fileStorageService.storeCv(mockFile, 5L)).thenReturn("/storage/cv_5_uuid.pdf");
        when(matchService.extractPdfText(fileBytes)).thenReturn("Fresh user CV text");
        when(matchService.extractProfile("Fresh user CV text")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.uploadCv("fresh@example.com", mockFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.isHasCv());
        assertEquals(2, result.getYearsExperience());
        assertEquals("junior", result.getRoleLevel());

        verify(fileStorageService, never()).deleteCv(any());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertEquals(user, profileCaptor.getValue().getUser());
        assertEquals("/storage/cv_5_uuid.pdf", profileCaptor.getValue().getCvFilePath());
    }

    // -------------------------------------------------------------------------
    // downloadCv
    // -------------------------------------------------------------------------

    @Test
    void downloadCv_success_returnsBytes() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .cvFilePath("/storage/cv_1_abc.pdf")
                .build();

        byte[] expectedBytes = "pdf-file-content".getBytes();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(fileStorageService.loadCv("/storage/cv_1_abc.pdf")).thenReturn(expectedBytes);

        // Act
        byte[] result = profileService.downloadCv("john@example.com");

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedBytes, result);

        verify(fileStorageService).loadCv("/storage/cv_1_abc.pdf");
    }

    @Test
    void downloadCv_noProfile_throwsException() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.downloadCv("john@example.com")
        );
        assertEquals("Profile not found", exception.getMessage());

        verify(fileStorageService, never()).loadCv(any());
    }

    @Test
    void downloadCv_noCvFilePath_throwsException() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .cvFilePath(null)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.downloadCv("john@example.com")
        );
        assertEquals("No CV uploaded", exception.getMessage());

        verify(fileStorageService, never()).loadCv(any());
    }

    // -------------------------------------------------------------------------
    // deleteCv
    // -------------------------------------------------------------------------

    @Test
    void deleteCv_withExistingCv_clearsAllCvData() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .cvFilePath("/storage/cv_1_abc.pdf")
                .cvRawText("Some CV text")
                .skills(List.of("java", "spring"))
                .yearsExperience(5)
                .roleLevel("mid")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        profileService.deleteCv("john@example.com");

        // Assert
        verify(fileStorageService).deleteCv("/storage/cv_1_abc.pdf");

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();
        assertNull(savedProfile.getCvFilePath());
        assertNull(savedProfile.getCvRawText());
        assertNull(savedProfile.getSkills());
        assertNull(savedProfile.getYearsExperience());
        assertNull(savedProfile.getRoleLevel());
    }

    @Test
    void deleteCv_noCvFilePath_noOp() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .cvFilePath(null)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        // Act
        profileService.deleteCv("john@example.com");

        // Assert
        verify(fileStorageService, never()).deleteCv(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void deleteCv_noProfile_throwsException() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> profileService.deleteCv("john@example.com")
        );
        assertEquals("Profile not found", exception.getMessage());

        verify(fileStorageService, never()).deleteCv(any());
        verify(profileRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // importLinkedInPdf
    // -------------------------------------------------------------------------

    @Test
    void importLinkedInPdf_mergeWithExistingSkills_deduplicates() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .skills(new ArrayList<>(List.of("java", "spring")))
                .yearsExperience(5)
                .roleLevel("mid")
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "linkedin-pdf-bytes".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "LinkedIn profile text",
                Set.of("java", "python", "docker"),
                8,
                "senior",
                Set.of("java", "python", "docker", "senior")
        );

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(matchService.extractPdfText(fileBytes)).thenReturn("LinkedIn profile text");
        when(matchService.extractProfile("LinkedIn profile text")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.importLinkedInPdf("john@example.com", mockFile);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();

        // Skills should be merged: java (existing), spring (existing), python (new), docker (new)
        List<String> savedSkills = savedProfile.getSkills();
        assertNotNull(savedSkills);
        assertTrue(savedSkills.contains("java"));
        assertTrue(savedSkills.contains("spring"));
        assertTrue(savedSkills.contains("python"));
        assertTrue(savedSkills.contains("docker"));
        // "java" should only appear once (deduplication via LinkedHashSet)
        assertEquals(savedSkills.size(), new HashSet<>(savedSkills).size());

        // yearsExperience already set, should NOT be overwritten
        assertEquals(5, savedProfile.getYearsExperience());
        // roleLevel already set, should NOT be overwritten
        assertEquals("mid", savedProfile.getRoleLevel());
    }

    @Test
    void importLinkedInPdf_noExistingSkills_setsFromLinkedIn() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .skills(null)
                .yearsExperience(null)
                .roleLevel(null)
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "linkedin-pdf-bytes".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "LinkedIn profile text",
                Set.of("react", "typescript", "node"),
                3,
                "junior",
                Set.of("react", "typescript", "node", "junior")
        );

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(matchService.extractPdfText(fileBytes)).thenReturn("LinkedIn profile text");
        when(matchService.extractProfile("LinkedIn profile text")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.importLinkedInPdf("john@example.com", mockFile);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();

        assertNotNull(savedProfile.getSkills());
        assertEquals(3, savedProfile.getSkills().size());
        assertTrue(savedProfile.getSkills().containsAll(List.of("react", "typescript", "node")));

        // yearsExperience was null, should be set from LinkedIn
        assertEquals(3, savedProfile.getYearsExperience());
        // roleLevel was null, should be set from LinkedIn
        assertEquals("junior", savedProfile.getRoleLevel());
    }

    @Test
    void importLinkedInPdf_noExistingProfile_createsNewProfile() throws IOException {
        // Arrange
        User user = User.builder()
                .id(7L)
                .email("new@example.com")
                .passwordHash("hashed")
                .firstName("New")
                .lastName("User")
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "linkedin-pdf-bytes".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "LinkedIn profile text",
                Set.of("aws", "terraform"),
                10,
                "senior",
                Set.of("aws", "terraform", "senior")
        );

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(matchService.extractPdfText(fileBytes)).thenReturn("LinkedIn profile text");
        when(matchService.extractProfile("LinkedIn profile text")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProfileDto result = profileService.importLinkedInPdf("new@example.com", mockFile);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();

        assertEquals(user, savedProfile.getUser());
        assertNotNull(savedProfile.getSkills());
        assertTrue(savedProfile.getSkills().containsAll(List.of("aws", "terraform")));
        assertEquals(10, savedProfile.getYearsExperience());
        assertEquals("senior", savedProfile.getRoleLevel());
    }

    @Test
    void importLinkedInPdf_existingYearsAndRole_preservesExisting() throws IOException {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .skills(new ArrayList<>(List.of("java")))
                .yearsExperience(5)
                .roleLevel("mid")
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        byte[] fileBytes = "linkedin-pdf".getBytes();
        when(mockFile.getBytes()).thenReturn(fileBytes);

        MatchService.CVProfile cvProfile = new MatchService.CVProfile(
                "LinkedIn text",
                Set.of("java", "kotlin"),
                10,
                "senior",
                Set.of("java", "kotlin", "senior")
        );

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(matchService.extractPdfText(fileBytes)).thenReturn("LinkedIn text");
        when(matchService.extractProfile("LinkedIn text")).thenReturn(cvProfile);
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        profileService.importLinkedInPdf("john@example.com", mockFile);

        // Assert
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();

        // yearsExperience was already set to 5, should NOT be overwritten with 10
        assertEquals(5, savedProfile.getYearsExperience());
        // roleLevel was already set to "mid", should NOT be overwritten with "senior"
        assertEquals("mid", savedProfile.getRoleLevel());
        // But skills should still be merged
        assertTrue(savedProfile.getSkills().contains("kotlin"));
    }
}
