package ee.itjobs.service;

import ee.itjobs.dto.auth.*;
import ee.itjobs.entity.EmailVerificationToken;
import ee.itjobs.entity.PasswordResetToken;
import ee.itjobs.entity.User;
import ee.itjobs.exception.DuplicateResourceException;
import ee.itjobs.repository.EmailVerificationTokenRepository;
import ee.itjobs.repository.PasswordResetTokenRepository;
import ee.itjobs.repository.UserRepository;
import ee.itjobs.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final TotpService totpService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
        user = userRepository.save(user);

        // Send verification email
        String verifyToken = UUID.randomUUID().toString();
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .token(verifyToken)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());
        emailService.sendVerificationEmail(user.getEmail(), verifyToken);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                return AuthResponse.builder()
                        .requiresTwoFactor(true)
                        .build();
            }
            if (!totpService.verifyCode(user.getTotpSecret(), request.getTotpCode())) {
                throw new IllegalArgumentException("Invalid 2FA code");
            }
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String email = jwtTokenProvider.getEmailFromToken(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildAuthResponse(user);
    }

    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isAdmin(user.getIsAdmin())
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verifyToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
        if (verifyToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }
        User user = verifyToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(verifyToken);
    }

    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(email, token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token has expired");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    public TotpSetupResponse setup2fa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);
        String qrCodeUri = totpService.getQrCodeUri(secret, email);
        return new TotpSetupResponse(secret, qrCodeUri);
    }

    public void enable2fa(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Invalid 2FA code");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    public void disable2fa(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("Invalid 2FA code");
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isAdmin(user.getIsAdmin())
                        .build())
                .build();
    }
}
