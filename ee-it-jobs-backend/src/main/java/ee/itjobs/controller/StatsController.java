package ee.itjobs.controller;

import ee.itjobs.service.StatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/skills")
    public ResponseEntity<List<Map<String, Object>>> getTopSkills(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(statsService.getTopSkills(limit));
    }

    @GetMapping("/sources")
    public ResponseEntity<List<Map<String, Object>>> getJobsBySource() {
        return ResponseEntity.ok(statsService.getJobsBySource());
    }

    @GetMapping("/trends")
    public ResponseEntity<List<Map<String, Object>>> getDailyTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.getDailyJobTrends(days));
    }
}
