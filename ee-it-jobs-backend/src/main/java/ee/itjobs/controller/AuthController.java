package ee.itjobs.controller;

import ee.itjobs.dto.auth.*;
import ee.itjobs.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        authService.requestPasswordReset(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TotpSetupResponse> setup2fa(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(authService.setup2fa(user.getUsername()));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, String>> verify2fa(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody TotpVerifyRequest request) {
        authService.enable2fa(user.getUsername(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "2FA enabled"));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2fa(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody TotpVerifyRequest request) {
        authService.disable2fa(user.getUsername(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "2FA disabled"));
    }
}
