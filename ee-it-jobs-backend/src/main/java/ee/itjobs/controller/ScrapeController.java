package ee.itjobs.controller;

import ee.itjobs.entity.ScrapeRun;
import ee.itjobs.repository.ScrapeRunRepository;
import ee.itjobs.service.ScrapeOrchestratorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scrape")
@RequiredArgsConstructor
@Tag(name = "Scraping")
public class ScrapeController {

    private final ScrapeOrchestratorService orchestratorService;
    private final ScrapeRunRepository scrapeRunRepository;

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeRun> triggerScrape() {
        ScrapeRun run = orchestratorService.triggerScrape("admin");
        return ResponseEntity.ok(run);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("isRunning", orchestratorService.isRunning());
        orchestratorService.getLatestRun().ifPresent(run -> {
            status.put("lastRun", Map.of(
                    "id", run.getId(),
                    "startedAt", run.getStartedAt(),
                    "completedAt", run.getCompletedAt() != null ? run.getCompletedAt() : "",
                    "status", run.getStatus(),
                    "totalJobs", run.getTotalJobs(),
                    "totalNewJobs", run.getTotalNewJobs(),
                    "totalErrors", run.getTotalErrors()
            ));
        });
        return ResponseEntity.ok(status);
    }

    @GetMapping("/runs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ScrapeRun>> getScrapeRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(scrapeRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(page, size)));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeRun> getScrapeRun(@PathVariable Long id) {
        return scrapeRunRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
