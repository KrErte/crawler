package ee.itjobs.service;

import ee.itjobs.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExpiryService {

    private final JobRepository jobRepository;

    @Value("${app.job-expiry-days:30}")
    private int expiryDays;

    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    @Transactional
    public void deactivateExpiredJobs() {
        LocalDate cutoff = LocalDate.now().minusDays(expiryDays);
        int deactivated = jobRepository.deactivateStaleJobs(cutoff);
        if (deactivated > 0) {
            log.info("Deactivated {} expired jobs (not scraped since {})", deactivated, cutoff);
        } else {
            log.debug("No expired jobs to deactivate");
        }
    }
}
