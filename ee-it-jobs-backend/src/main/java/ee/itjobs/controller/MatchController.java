package ee.itjobs.controller;

import ee.itjobs.dto.match.JobMatchScoreDto;
import ee.itjobs.dto.match.MatchResultDto;
import ee.itjobs.service.MatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
@Tag(name = "CV Matching")
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<List<MatchResultDto>> match(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "20") int topN) throws IOException {
        return ResponseEntity.ok(matchService.matchJobs(file.getBytes(), topN));
    }

    @PostMapping("/profile")
    public ResponseEntity<List<MatchResultDto>> matchFromProfile(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int topN) {
        return ResponseEntity.ok(matchService.matchJobsFromProfile(authentication.getName(), topN));
    }

    @PostMapping("/scores")
    public ResponseEntity<List<JobMatchScoreDto>> getMatchScores(
            Authentication authentication,
            @RequestBody List<Long> jobIds) {
        return ResponseEntity.ok(matchService.matchJobsByIds(authentication.getName(), jobIds));
    }
}
