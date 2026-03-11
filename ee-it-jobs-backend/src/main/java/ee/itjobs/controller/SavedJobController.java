package ee.itjobs.controller;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.service.SavedJobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/saved-jobs")
@RequiredArgsConstructor
@Tag(name = "Saved Jobs")
public class SavedJobController {

    private final SavedJobService savedJobService;

    @GetMapping
    public ResponseEntity<List<JobDto>> getSavedJobs(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(savedJobService.getSavedJobs(user.getUsername()));
    }

    @GetMapping("/ids")
    public ResponseEntity<Set<Long>> getSavedJobIds(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(savedJobService.getSavedJobIds(user.getUsername()));
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<Void> saveJob(@AuthenticationPrincipal UserDetails user, @PathVariable Long jobId) {
        savedJobService.saveJob(user.getUsername(), jobId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> unsaveJob(@AuthenticationPrincipal UserDetails user, @PathVariable Long jobId) {
        savedJobService.unsaveJob(user.getUsername(), jobId);
        return ResponseEntity.noContent().build();
    }
}
