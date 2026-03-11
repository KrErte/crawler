package ee.itjobs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.itjobs.dto.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        String unique = String.valueOf(System.nanoTime());
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("jobflow-" + unique + "@test.com");
        reg.setPassword("TestPassword123!");
        reg.setFirstName("Job");
        reg.setLastName("Tester");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void getJobs_publicEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/api/jobs").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getJobFilters_returns200() throws Exception {
        mockMvc.perform(get("/api/jobs/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies").isArray())
                .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    void scrapeStatus_publicEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/api/scrape/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRunning").isBoolean());
    }

    @Test
    void savedJobs_requiresAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/saved-jobs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSavedJobIds_requiresAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/saved-jobs/ids")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
