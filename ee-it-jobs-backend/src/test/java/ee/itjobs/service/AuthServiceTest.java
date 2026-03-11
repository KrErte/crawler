package ee.itjobs.service;

import ee.itjobs.dto.auth.AuthResponse;
import ee.itjobs.dto.auth.LoginRequest;
import ee.itjobs.dto.auth.RegisterRequest;
import ee.itjobs.entity.EmailVerificationToken;
import ee.itjobs.entity.PasswordResetToken;
import ee.itjobs.entity.User;
import ee.itjobs.exception.DuplicateResourceException;
import ee.itjobs.repository.EmailVerificationTokenRepository;
import ee.itjobs.repository.PasswordResetTokenRepository;
import ee.itjobs.repository.UserRepository;
import ee.itjobs.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_newEmail_success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .isAdmin(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken("test@example.com")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("test@example.com")).thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("John", response.getUser().getFirstName());

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );
        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_validCredentials_returnsToken() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .isAdmin(false)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("user@example.com", "password123"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken("user@example.com")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("user@example.com")).thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("user@example.com", response.getUser().getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("user@example.com");
    }

    @Test
    void login_userNotFound_throwsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("nonexistent@example.com", "password123"));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void resetPassword_validToken_success() {
        // Arrange
        String token = "valid-reset-token";
        String newPassword = "newPassword123";

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("oldEncodedPassword")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(token))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        // Act
        authService.resetPassword(token, newPassword);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newEncodedPassword", userCaptor.getValue().getPasswordHash());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertTrue(tokenCaptor.getValue().getUsed());
    }
}
