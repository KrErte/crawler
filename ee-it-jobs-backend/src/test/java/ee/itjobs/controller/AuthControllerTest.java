package ee.itjobs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.itjobs.dto.auth.AuthResponse;
import ee.itjobs.dto.auth.LoginRequest;
import ee.itjobs.dto.auth.RegisterRequest;
import ee.itjobs.security.JwtAuthFilter;
import ee.itjobs.security.JwtTokenProvider;
import ee.itjobs.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private AuthResponse createAuthResponse() {
        return AuthResponse.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .user(AuthResponse.UserDto.builder()
                        .id(1L)
                        .email("user@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .isAdmin(false)
                        .build())
                .build();
    }

    @Test
    void login_validRequest_returns200() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(createAuthResponse());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("John"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void register_validRequest_returns200() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");

        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .user(AuthResponse.UserDto.builder()
                        .id(2L)
                        .email("newuser@example.com")
                        .firstName("Jane")
                        .lastName("Smith")
                        .isAdmin(false)
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("Jane"))
                .andExpect(jsonPath("$.user.lastName").value("Smith"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void verifyEmail_validToken_returns200() throws Exception {
        // Arrange
        doNothing().when(authService).verifyEmail("valid-token-123");

        // Act & Assert
        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "valid-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(authService).verifyEmail("valid-token-123");
    }

    @Test
    void forgotPassword_returns200() throws Exception {
        // Arrange
        doNothing().when(authService).requestPasswordReset("user@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email exists, a reset link has been sent"));

        verify(authService).requestPasswordReset("user@example.com");
    }

    @Test
    void resetPassword_validRequest_returns200() throws Exception {
        // Arrange
        doNothing().when(authService).resetPassword("reset-token", "newPassword123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "reset-token", "newPassword", "newPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));

        verify(authService).resetPassword("reset-token", "newPassword123");
    }
}
