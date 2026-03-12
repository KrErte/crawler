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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Cacheable(value = "recommendations", key = "{#email, #limit}")
    public List<JobDto> getRecommendations(String email, int limit) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Application> applications = applicationRepository.findByUserIdOrderByAppliedAtDesc(user.getId());
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract patterns from applied jobs
        Set<Long> appliedJobIds = applications.stream()
                .map(a -> a.getJob().getId())
                .collect(Collectors.toSet());

        Map<String, Integer> companyFrequency = new HashMap<>();
        Map<String, Integer> skillFrequency = new HashMap<>();
        Set<String> titleKeywords = new HashSet<>();

        for (Application app : applications) {
            Job job = app.getJob();
            companyFrequency.merge(job.getCompany(), 1, Integer::sum);

            if (job.getSkills() != null) {
                for (String skill : job.getSkills()) {
                    skillFrequency.merge(skill.toLowerCase(), 1, Integer::sum);
                }
            }

            // Extract title keywords
            for (String word : job.getTitle().toLowerCase().split("\\s+")) {
                if (word.length() > 3) {
                    titleKeywords.add(word);
                }
            }
        }

        // Find similar active jobs not already applied to
        List<Job> activeJobs = jobRepository.findByIsActiveTrueOrderByDateScrapedDesc();

        // Score each job by similarity to application history
        List<Map.Entry<Job, Double>> scoredJobs = activeJobs.stream()
                .filter(job -> !appliedJobIds.contains(job.getId()))
                .map(job -> {
                    double score = 0;

                    // Company match (applied to same company before)
                    if (companyFrequency.containsKey(job.getCompany())) {
                        score += 10 * companyFrequency.get(job.getCompany());
                    }

                    // Skill overlap
                    if (job.getSkills() != null) {
                        for (String skill : job.getSkills()) {
                            Integer freq = skillFrequency.get(skill.toLowerCase());
                            if (freq != null) {
                                score += 5 * freq;
                            }
                        }
                    }

                    // Title keyword match
                    String titleLower = job.getTitle().toLowerCase();
                    for (String keyword : titleKeywords) {
                        if (titleLower.contains(keyword)) {
                            score += 3;
                        }
                    }

                    return Map.entry(job, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Job, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();

        return scoredJobs.stream()
                .map(e -> jobMapper.toDto(e.getKey()))
                .toList();
    }
}
