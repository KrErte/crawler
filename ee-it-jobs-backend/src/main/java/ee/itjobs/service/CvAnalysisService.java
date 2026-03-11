package ee.itjobs.service;

import ee.itjobs.dto.profile.CvAnalysisDto;
import ee.itjobs.entity.User;
import ee.itjobs.entity.UserProfile;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserProfileRepository;
import ee.itjobs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CvAnalysisService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;

    public CvAnalysisDto analyze(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (profile.getCvRawText() == null || profile.getCvRawText().isBlank()) {
            throw new ResourceNotFoundException("No CV uploaded");
        }

        List<String> detectedSkills = profile.getSkills() != null ? profile.getSkills() : List.of();
        Integer yearsExperience = profile.getYearsExperience();
        String roleLevel = profile.getRoleLevel();

        // Get top in-demand skills from active jobs
        List<Map<String, Object>> topSkillRows = jdbcTemplate.queryForList(
                "SELECT skill, COUNT(*) as count FROM jobs, jsonb_array_elements_text(skills) AS skill " +
                "WHERE is_active = true GROUP BY skill ORDER BY count DESC LIMIT 20");

        List<String> topSkills = topSkillRows.stream()
                .map(row -> (String) row.get("skill"))
                .collect(Collectors.toList());

        Set<String> detectedLower = detectedSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> missingInDemand = topSkills.stream()
                .filter(skill -> !detectedLower.contains(skill.toLowerCase()))
                .limit(10)
                .collect(Collectors.toList());

        // Completeness score
        int completeness = calculateCompleteness(profile, detectedSkills);

        // Suggestions
        List<String> suggestions = buildSuggestions(profile, detectedSkills, missingInDemand, yearsExperience);

        // Count active and matching jobs
        long totalActive = jobRepository.countByIsActiveTrue();
        long matchingJobs = countMatchingJobs(detectedSkills);

        return CvAnalysisDto.builder()
                .completenessScore(completeness)
                .detectedSkills(detectedSkills)
                .missingInDemandSkills(missingInDemand)
                .suggestions(suggestions)
                .yearsExperience(yearsExperience)
                .roleLevel(roleLevel)
                .totalActiveJobs((int) totalActive)
                .matchingJobs((int) matchingJobs)
                .build();
    }

    private int calculateCompleteness(UserProfile profile, List<String> skills) {
        int score = 0;

        // Has CV text (20 points)
        if (profile.getCvRawText() != null && !profile.getCvRawText().isBlank()) {
            score += 20;
        }

        // Has skills detected (30 points, scaled by count)
        if (!skills.isEmpty()) {
            score += Math.min(skills.size() * 5, 30);
        }

        // Has years of experience (15 points)
        if (profile.getYearsExperience() != null) {
            score += 15;
        }

        // Has role level (10 points)
        if (profile.getRoleLevel() != null) {
            score += 10;
        }

        // User info completeness
        User user = profile.getUser();
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) score += 5;
        if (user.getLastName() != null && !user.getLastName().isBlank()) score += 5;
        if (user.getPhone() != null && !user.getPhone().isBlank()) score += 5;
        if (user.getLinkedinUrl() != null && !user.getLinkedinUrl().isBlank()) score += 5;

        // Cover letter (5 points)
        if (profile.getCoverLetter() != null && !profile.getCoverLetter().isBlank()) {
            score += 5;
        }

        return Math.min(score, 100);
    }

    private List<String> buildSuggestions(UserProfile profile, List<String> skills,
                                          List<String> missingSkills, Integer yearsExperience) {
        List<String> suggestions = new ArrayList<>();

        if (skills.size() < 3) {
            suggestions.add("Your CV has few detected skills. Consider adding a dedicated 'Technical Skills' section.");
        }

        if (yearsExperience == null) {
            suggestions.add("Include years of experience clearly (e.g., '5+ years of experience').");
        }

        if (profile.getCoverLetter() == null || profile.getCoverLetter().isBlank()) {
            suggestions.add("Add a cover letter to strengthen your applications.");
        }

        User user = profile.getUser();
        if (user.getLinkedinUrl() == null || user.getLinkedinUrl().isBlank()) {
            suggestions.add("Add your LinkedIn URL to increase credibility with employers.");
        }

        if (!missingSkills.isEmpty()) {
            String top3 = missingSkills.stream().limit(3).collect(Collectors.joining(", "));
            suggestions.add("Consider learning these in-demand skills: " + top3 + ".");
        }

        if (profile.getCvRawText() != null && profile.getCvRawText().length() < 500) {
            suggestions.add("Your CV seems short. Add more detail about your projects and accomplishments.");
        }

        return suggestions;
    }

    private long countMatchingJobs(List<String> skills) {
        if (skills == null || skills.isEmpty()) return 0;

        String placeholders = skills.stream()
                .map(s -> "?")
                .collect(Collectors.joining(", "));

        String sql = "SELECT COUNT(DISTINCT j.id) FROM jobs j, jsonb_array_elements_text(j.skills) AS skill " +
                     "WHERE j.is_active = true AND LOWER(skill) IN (" + placeholders + ")";

        Object[] params = skills.stream().map(String::toLowerCase).toArray();

        Long result = jdbcTemplate.queryForObject(sql, Long.class, params);
        return result != null ? result : 0;
    }
}
