package ee.itjobs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.itjobs.dto.auth.LoginRequest;
import ee.itjobs.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthFlow_registerLoginRefresh() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("integration@test.com");
        registerRequest.setPassword("TestPassword123!");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("integration@test.com"))
                .andReturn();

        String responseBody = registerResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();

        // Step 2: Access protected endpoint with token
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@test.com"));

        // Step 3: Login with same credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("integration@test.com");
        loginRequest.setPassword("TestPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Step 4: Refresh token
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@test.com");
        request.setPassword("TestPassword123!");
        request.setFirstName("First");
        request.setLastName("User");

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void accessProtectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }
}
