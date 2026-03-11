package ee.itjobs.service;

import ee.itjobs.entity.Job;
import ee.itjobs.entity.ScrapeRun;
import ee.itjobs.repository.ScrapeRunRepository;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.ScrapeResult;
import ee.itjobs.scraper.ScraperRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeOrchestratorService {

    private final ScraperRegistry scraperRegistry;
    private final DeduplicationService deduplicationService;
    private final ScrapeRunRepository scrapeRunRepository;

    @Value("${app.scraper.max-concurrency}")
    private int maxConcurrency;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public boolean isRunning() {
        return isRunning.get();
    }

    public Optional<ScrapeRun> getLatestRun() {
        return scrapeRunRepository.findTopByOrderByStartedAtDesc();
    }

    @Scheduled(cron = "${app.scraper.cron}")
    public void scheduledScrape() {
        log.info("Starting scheduled scrape");
        triggerScrape("scheduler");
    }

    public ScrapeRun triggerScrape(String triggeredBy) {
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Scrape already in progress");
        }

        ScrapeRun run = ScrapeRun.builder()
                .startedAt(LocalDateTime.now())
                .status("running")
                .triggeredBy(triggeredBy)
                .build();
        run = scrapeRunRepository.save(run);
        final ScrapeRun savedRun = run;

        CompletableFuture.runAsync(() -> {
            try {
                executeScrape(savedRun);
            } catch (Exception e) {
                log.error("Scrape failed", e);
                savedRun.setStatus("failed");
                savedRun.setCompletedAt(LocalDateTime.now());
                scrapeRunRepository.save(savedRun);
            } finally {
                isRunning.set(false);
            }
        });

        return run;
    }

    private void executeScrape(ScrapeRun run) {
        List<BaseScraper> scrapers = scraperRegistry.getActiveScrapers();
        ExecutorService executor = Executors.newFixedThreadPool(maxConcurrency);
        AtomicInteger totalJobs = new AtomicInteger(0);
        AtomicInteger totalNew = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);
        Map<String, Object> sourceStats = Collections.synchronizedMap(new LinkedHashMap<>());

        List<CompletableFuture<Void>> futures = scrapers.stream()
                .map(scraper -> CompletableFuture.runAsync(() -> {
                    ScrapeResult result = scraper.run();
                    int newCount = 0;
                    for (Job job : result.getJobs()) {
                        if (!JobService.isItRelated(job.getTitle(), job.getDepartment())) {
                            continue;
                        }
                        job.setScrapeRun(run);
                        boolean isNew = deduplicationService.upsertJob(job);
                        if (isNew) newCount++;
                    }
                    totalJobs.addAndGet(result.getJobs().size());
                    totalNew.addAndGet(newCount);
                    totalErrors.addAndGet(result.getErrors().size());

                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("total", result.getJobs().size());
                    stats.put("new", newCount);
                    stats.put("errors", result.getErrors().size());
                    stats.put("duration", result.getDurationSeconds());
                    sourceStats.put(scraper.getName(), stats);
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        run.setCompletedAt(LocalDateTime.now());
        run.setStatus("completed");
        run.setTotalJobs(totalJobs.get());
        run.setTotalNewJobs(totalNew.get());
        run.setTotalErrors(totalErrors.get());
        run.setSourceStats(sourceStats);
        scrapeRunRepository.save(run);

        log.info("Scrape completed: {} total, {} new, {} errors",
                totalJobs.get(), totalNew.get(), totalErrors.get());
    }
}
