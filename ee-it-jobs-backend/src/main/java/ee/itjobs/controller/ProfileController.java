package ee.itjobs.controller;

import ee.itjobs.dto.profile.CvAnalysisDto;
import ee.itjobs.dto.profile.CvBuilderRequest;
import ee.itjobs.dto.profile.ProfileDto;
import ee.itjobs.dto.profile.ProfileUpdateRequest;
import ee.itjobs.service.CvAnalysisService;
import ee.itjobs.service.CvBuilderService;
import ee.itjobs.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profileService;
    private final CvBuilderService cvBuilderService;
    private final CvAnalysisService cvAnalysisService;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(profileService.getProfile(user.getUsername()));
    }

    @PutMapping
    public ResponseEntity<ProfileDto> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(user.getUsername(), request));
    }

    @PostMapping("/cv")
    public ResponseEntity<ProfileDto> uploadCv(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(profileService.uploadCv(user.getUsername(), file));
    }

    @GetMapping("/cv")
    public ResponseEntity<byte[]> downloadCv(@AuthenticationPrincipal UserDetails user) throws IOException {
        byte[] data = profileService.downloadCv(user.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cv.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @DeleteMapping("/cv")
    public ResponseEntity<Void> deleteCv(@AuthenticationPrincipal UserDetails user) throws IOException {
        profileService.deleteCv(user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-linkedin")
    public ResponseEntity<ProfileDto> importLinkedIn(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(profileService.importLinkedInPdf(user.getUsername(), file));
    }

    @GetMapping("/cv-analysis")
    public ResponseEntity<CvAnalysisDto> getCvAnalysis(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(cvAnalysisService.analyze(user.getUsername()));
    }

    @PostMapping("/cv/build")
    public ResponseEntity<byte[]> buildCv(
            @RequestBody CvBuilderRequest request) throws IOException {
        byte[] pdf = cvBuilderService.generateCv(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cv.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/cv/build-and-save")
    public ResponseEntity<ProfileDto> buildAndSaveCv(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody CvBuilderRequest request) throws IOException {
        return ResponseEntity.ok(cvBuilderService.generateAndSaveCv(user.getUsername(), request));
    }
}
