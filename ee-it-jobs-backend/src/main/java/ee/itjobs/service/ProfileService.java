package ee.itjobs.service;

import ee.itjobs.dto.profile.ProfileDto;
import ee.itjobs.dto.profile.ProfileUpdateRequest;
import ee.itjobs.entity.User;
import ee.itjobs.entity.UserProfile;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.repository.UserProfileRepository;
import ee.itjobs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final FileStorageService fileStorageService;
    private final MatchService matchService;

    public ProfileDto getProfile(String email) {
        User user = findUserByEmail(email);
        UserProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return buildProfileDto(user, profile);
    }

    @Transactional
    public ProfileDto updateProfile(String email, ProfileUpdateRequest request) {
        User user = findUserByEmail(email);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setLinkedinUrl(request.getLinkedinUrl());
        userRepository.save(user);

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(UserProfile.builder().user(user).build());
        profile.setCoverLetter(request.getCoverLetter());
        if (request.getEmailAlerts() != null) {
            profile.setEmailAlerts(request.getEmailAlerts());
        }
        if (request.getAlertThreshold() != null) {
            profile.setAlertThreshold(request.getAlertThreshold());
        }
        if (request.getNotificationPreferences() != null) {
            profile.setNotificationPreferences(request.getNotificationPreferences());
        }
        profileRepository.save(profile);

        return buildProfileDto(user, profile);
    }

    @Transactional
    public ProfileDto uploadCv(String email, MultipartFile file) throws IOException {
        User user = findUserByEmail(email);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(UserProfile.builder().user(user).build());

        // Delete old CV if exists
        if (profile.getCvFilePath() != null) {
            fileStorageService.deleteCv(profile.getCvFilePath());
        }

        String path = fileStorageService.storeCv(file, user.getId());
        profile.setCvFilePath(path);

        // Extract text and analyze
        String rawText = matchService.extractPdfText(file.getBytes());
        profile.setCvRawText(rawText);

        var cvProfile = matchService.extractProfile(rawText);
        profile.setSkills(new java.util.ArrayList<>(cvProfile.skills()));
        profile.setYearsExperience(cvProfile.yearsExperience());
        profile.setRoleLevel(cvProfile.roleLevel());

        profileRepository.save(profile);
        return buildProfileDto(user, profile);
    }

    public byte[] downloadCv(String email) throws IOException {
        User user = findUserByEmail(email);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        if (profile.getCvFilePath() == null) {
            throw new ResourceNotFoundException("No CV uploaded");
        }
        return fileStorageService.loadCv(profile.getCvFilePath());
    }

    @Transactional
    public void deleteCv(String email) throws IOException {
        User user = findUserByEmail(email);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        if (profile.getCvFilePath() != null) {
            fileStorageService.deleteCv(profile.getCvFilePath());
            profile.setCvFilePath(null);
            profile.setCvRawText(null);
            profile.setSkills(null);
            profile.setYearsExperience(null);
            profile.setRoleLevel(null);
            profileRepository.save(profile);
        }
    }

    @Transactional
    public ProfileDto importLinkedInPdf(String email, MultipartFile file) throws IOException {
        User user = findUserByEmail(email);
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(UserProfile.builder().user(user).build());

        String rawText = matchService.extractPdfText(file.getBytes());
        var cvProfile = matchService.extractProfile(rawText);

        // Merge skills
        if (profile.getSkills() != null) {
            java.util.Set<String> merged = new java.util.LinkedHashSet<>(profile.getSkills());
            merged.addAll(cvProfile.skills());
            profile.setSkills(new java.util.ArrayList<>(merged));
        } else {
            profile.setSkills(new java.util.ArrayList<>(cvProfile.skills()));
        }

        // Update years and role level if not already set
        if (profile.getYearsExperience() == null && cvProfile.yearsExperience() != null) {
            profile.setYearsExperience(cvProfile.yearsExperience());
        }
        if (profile.getRoleLevel() == null && cvProfile.roleLevel() != null) {
            profile.setRoleLevel(cvProfile.roleLevel());
        }

        profileRepository.save(profile);
        return buildProfileDto(user, profile);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProfileDto buildProfileDto(User user, UserProfile profile) {
        ProfileDto.ProfileDtoBuilder builder = ProfileDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .linkedinUrl(user.getLinkedinUrl());
        if (profile != null) {
            builder.coverLetter(profile.getCoverLetter())
                    .skills(profile.getSkills())
                    .preferences(profile.getPreferences())
                    .cvRawText(profile.getCvRawText())
                    .yearsExperience(profile.getYearsExperience())
                    .roleLevel(profile.getRoleLevel())
                    .hasCv(profile.getCvFilePath() != null)
                    .emailAlerts(profile.getEmailAlerts())
                    .alertThreshold(profile.getAlertThreshold())
                    .notificationPreferences(profile.getNotificationPreferences());
        }
        return builder.build();
    }
}
