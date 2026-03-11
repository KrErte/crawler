package ee.itjobs.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ee_it_jobs_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.jwt.secret", () -> "test-secret-key-that-is-at-least-64-characters-long-for-hmac-sha-256-signing");
        registry.add("app.jwt.expiration-ms", () -> "86400000");
        registry.add("app.jwt.refresh-expiration-ms", () -> "604800000");
        registry.add("app.scraper.max-concurrency", () -> "2");
        registry.add("app.scraper.cron", () -> "-");
        registry.add("app.storage.cv-dir", () -> System.getProperty("java.io.tmpdir") + "/cv-test");
    }
}
