package ee.itjobs.controller;

import ee.itjobs.dto.application.*;
import ee.itjobs.enums.ApplicationStatus;
import ee.itjobs.service.ApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getApplications(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.getApplications(user.getUsername(), status));
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.createApplication(user.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplication(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.updateApplication(user.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        applicationService.deleteApplication(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{jobId}")
    public ResponseEntity<Map<String, Boolean>> checkApplication(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long jobId) {
        boolean exists = applicationService.existsForJob(user.getUsername(), jobId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportApplications(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "csv") String format) throws IOException {
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = applicationService.exportToPdf(user.getUsername());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }
        byte[] csv = applicationService.exportToCsv(user.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
