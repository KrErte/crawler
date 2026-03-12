package ee.itjobs.scraper;

import ee.itjobs.entity.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BaseScraperTest {

    private TestScraper scraper;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(RateLimiter.class);
        scraper = new TestScraper(rateLimiter);
    }

    @Test
    void buildJob_dedupKeyFormat() {
        Job job = scraper.buildJob("Java Developer", "Wise", "Tallinn",
                "https://example.com", "Description", null, null);

        assertEquals("wise|java developer|tallinn", job.getDedupKey());
    }

    @Test
    void buildJob_dedupKeyNullLocation() {
        Job job = scraper.buildJob("Java Developer", "Wise", null,
                "https://example.com", "Description", null, null);

        assertEquals("wise|java developer|", job.getDedupKey());
    }

    @Test
    void buildJob_descriptionSnippetTruncation() {
        String longDesc = "a".repeat(300);
        Job job = scraper.buildJob("Title", "Company", "Location",
                "https://example.com", longDesc, null, null);

        assertEquals(200, job.getDescriptionSnippet().length());
        assertEquals(300, job.getFullDescription().length());
    }

    @Test
    void buildJob_salaryParsed() {
        Job job = scraper.buildJob("Title", "Company", "Location",
                "https://example.com", "Desc", "3000-5000 EUR", null);

        assertNotNull(job.getSalaryMin());
        assertNotNull(job.getSalaryMax());
        assertEquals("EUR", job.getSalaryCurrency());
    }

    @Test
    void containsEstoniaLocation_validLocations() {
        assertTrue(scraper.containsEstoniaLocation("Tallinn, Estonia"));
        assertTrue(scraper.containsEstoniaLocation("Tartu"));
        assertTrue(scraper.containsEstoniaLocation("Pärnu"));
        assertTrue(scraper.containsEstoniaLocation("Narva"));
        assertTrue(scraper.containsEstoniaLocation("Eesti"));
    }

    @Test
    void containsEstoniaLocation_invalidLocations() {
        assertFalse(scraper.containsEstoniaLocation("Helsinki, Finland"));
        assertFalse(scraper.containsEstoniaLocation("Riga, Latvia"));
        assertFalse(scraper.containsEstoniaLocation(null));
    }

    @Test
    void run_scrapeFailure_recordsError() {
        scraper.shouldFail = true;
        ScrapeResult result = scraper.run();

        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getJobs().isEmpty());
    }

    @Test
    void run_scrapeSuccess_recordsJobs() {
        scraper.shouldFail = false;
        ScrapeResult result = scraper.run();

        assertTrue(result.getErrors().isEmpty());
        assertEquals("test-source", result.getSource());
        assertTrue(result.getDurationSeconds() >= 0);
    }

    // Test subclass for testing BaseScraper
    private static class TestScraper extends BaseScraper {
        boolean shouldFail = false;

        TestScraper(RateLimiter rateLimiter) {
            super(org.springframework.web.reactive.function.client.WebClient.builder().build(), rateLimiter);
        }

        @Override
        public String getName() { return "TestScraper"; }

        @Override
        public String getSource() { return "test-source"; }

        @Override
        protected List<Job> scrape() throws Exception {
            if (shouldFail) throw new RuntimeException("Scrape error");
            return List.of();
        }
    }
}
