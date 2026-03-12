package ee.itjobs.scraper;

import ee.itjobs.entity.ScraperConfig;
import ee.itjobs.repository.ScraperConfigRepository;
import ee.itjobs.scraper.ats.*;
import ee.itjobs.scraper.custom.*;
import ee.itjobs.scraper.portal.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScraperRegistry {

    private final ScraperConfigRepository configRepository;
    private final WebClient webClient;

    @Value("${app.scraper.rate-limit}")
    private double rateLimit;

    public List<BaseScraper> getActiveScrapers() {
        List<ScraperConfig> configs = configRepository.findByIsActiveTrue();
        List<BaseScraper> scrapers = new ArrayList<>();
        RateLimiter rateLimiter = new RateLimiter(rateLimit);

        for (ScraperConfig config : configs) {
            try {
                BaseScraper scraper = createScraper(config, rateLimiter);
                if (scraper != null) {
                    scrapers.add(scraper);
                }
            } catch (Exception e) {
                log.error("Failed to create scraper for config {}: {}", config.getId(), e.getMessage());
            }
        }

        log.info("Loaded {} active scrapers", scrapers.size());
        return scrapers;
    }

    private BaseScraper createScraper(ScraperConfig config, RateLimiter rateLimiter) {
        Map<String, String> json = config.getConfigJson();
        if (json == null) json = Map.of();

        return switch (config.getScraperType()) {
            case "CV_EE" -> new CvEeScraper(webClient, rateLimiter);
            case "CV_KESKUS" -> new CvKeskusScraper(webClient, rateLimiter);
            case "KANDIDEERI" -> new KandideeriScraper(webClient, rateLimiter);
            case "LEVER" -> new LeverScraper(webClient, rateLimiter,
                    json.get("company_slug"), config.getCompanyName(),
                    json.getOrDefault("api_host", "api.lever.co"));
            case "GREENHOUSE" -> new GreenhouseScraper(webClient, rateLimiter,
                    json.get("board_token"), config.getCompanyName());
            case "SMART_RECRUITERS" -> new SmartRecruitersScraper(webClient, rateLimiter,
                    json.get("company_id"), config.getCompanyName());
            case "WORKABLE" -> new WorkableScraper(webClient, rateLimiter,
                    json.get("account_slug"), config.getCompanyName());
            case "TEAMTAILOR" -> new TeamtailorScraper(webClient, rateLimiter,
                    json.get("base_url"), config.getCompanyName());
            case "NORTAL" -> new NortalScraper(webClient, rateLimiter);
            case "HELMES" -> new HelmesScraper(webClient, rateLimiter);
            case "MEETFRANK" -> new MeetFrankScraper(webClient, rateLimiter);
            case "INDEED" -> new IndeedScraper(webClient, rateLimiter);
            case "TOOTUKASSA" -> new TootukassaScraper(webClient, rateLimiter);
            case "ASHBY" -> new AshbyScraper(webClient, rateLimiter,
                    json.get("board_name"), config.getCompanyName());
            default -> {
                log.warn("Unknown scraper type: {}", config.getScraperType());
                yield null;
            }
        };
    }
}
