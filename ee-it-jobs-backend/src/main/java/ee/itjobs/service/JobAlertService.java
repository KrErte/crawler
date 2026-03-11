package ee.itjobs.service;

import ee.itjobs.dto.match.MatchResultDto;
import ee.itjobs.entity.UserProfile;
import ee.itjobs.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobAlertService {

    private final UserProfileRepository userProfileRepository;
    private final MatchService matchService;
    private final EmailService emailService;

    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void sendDailyAlerts() {
        log.info("Starting daily job alerts...");
        List<UserProfile> profiles = userProfileRepository.findByEmailAlertsTrue();

        for (UserProfile profile : profiles) {
            if (profile.getCvRawText() == null || profile.getCvRawText().isBlank()) continue;

            try {
                String email = profile.getUser().getEmail();
                List<MatchResultDto> matches = matchService.matchJobsFromProfile(email, 10);

                int threshold = profile.getAlertThreshold() != null ? profile.getAlertThreshold() : 70;
                List<MatchResultDto> goodMatches = matches.stream()
                        .filter(m -> m.getMatchPercentage() >= threshold)
                        .filter(m -> matchesNotificationPreferences(profile, m))
                        .collect(Collectors.toList());

                if (!goodMatches.isEmpty()) {
                    StringBuilder body = new StringBuilder();
                    body.append("Hi! Here are your top job matches today:\n\n");
                    for (MatchResultDto match : goodMatches) {
                        body.append(String.format("- %s at %s (%d%% match)\n  %s\n\n",
                                match.getJob().getTitle(),
                                match.getJob().getCompany(),
                                match.getMatchPercentage(),
                                match.getJob().getUrl()));
                    }
                    body.append("Good luck with your job search!\n\nEE IT Jobs");

                    emailService.sendJobAlertEmail(email,
                            "EE IT Jobs - " + goodMatches.size() + " new matches for you!",
                            body.toString());

                    profile.setLastAlertSentAt(LocalDateTime.now());
                    userProfileRepository.save(profile);
                    log.info("Sent {} job alerts to {}", goodMatches.size(), email);
                }
            } catch (Exception e) {
                log.error("Failed to send alerts for profile {}", profile.getId(), e);
            }
        }
        log.info("Daily job alerts completed.");
    }

    @SuppressWarnings("unchecked")
    private boolean matchesNotificationPreferences(UserProfile profile, MatchResultDto match) {
        Map<String, Object> prefs = profile.getNotificationPreferences();
        if (prefs == null || prefs.isEmpty()) return true;

        var job = match.getJob();

        // Filter by workplace type preference
        List<String> workplaceTypes = (List<String>) prefs.get("workplaceTypes");
        if (workplaceTypes != null && !workplaceTypes.isEmpty() && job.getWorkplaceType() != null) {
            if (!workplaceTypes.contains(job.getWorkplaceType())) return false;
        }

        // Filter by minimum salary
        Number minSalary = (Number) prefs.get("minSalary");
        if (minSalary != null && job.getSalaryMax() != null) {
            if (job.getSalaryMax() < minSalary.intValue()) return false;
        }

        return true;
    }
}
