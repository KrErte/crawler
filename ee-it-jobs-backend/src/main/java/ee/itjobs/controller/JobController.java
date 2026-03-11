package ee.itjobs.controller;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.job.JobFilterDto;
import ee.itjobs.service.JobService;
import ee.itjobs.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs")
public class JobController {

    private final JobService jobService;
    private final TranslationService translationService;

    @GetMapping
    @Operation(summary = "Get paginated job listings with filters")
    public ResponseEntity<Page<JobDto>> getJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String workplaceType,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(defaultValue = "dateScraped") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobService.getJobs(search, company, source,
                workplaceType, jobType, skills, salaryMin, salaryMax, sortBy, sortDir, page, size));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Get search suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        return ResponseEntity.ok(jobService.getSuggestions(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobDto> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @GetMapping("/filters")
    @Operation(summary = "Get available filter options")
    public ResponseEntity<JobFilterDto> getFilters() {
        return ResponseEntity.ok(jobService.getFilters());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get job statistics (admin)")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(jobService.getJobStats());
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle job active status (admin)")
    public ResponseEntity<Void> toggleJobActive(@PathVariable Long id, @RequestParam boolean active) {
        jobService.setJobActive(id, active);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/translate")
    @Operation(summary = "Get translated job description")
    public ResponseEntity<Map<String, String>> translateJob(
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String targetLang) {
        JobDto job = jobService.getJob(id);
        String description = job.getFullDescription() != null ? job.getFullDescription() : job.getDescriptionSnippet();
        if (description == null || description.isBlank()) {
            return ResponseEntity.ok(Map.of("title", job.getTitle(), "description", ""));
        }

        String sourceLang = translationService.isEstonian(description) ? "et" : "en";
        String translatedTitle = translationService.translate(job.getTitle(), sourceLang, targetLang);
        String translatedDesc = translationService.translate(description, sourceLang, targetLang);

        return ResponseEntity.ok(Map.of(
                "title", translatedTitle,
                "description", translatedDesc,
                "detectedLang", sourceLang
        ));
    }
}
