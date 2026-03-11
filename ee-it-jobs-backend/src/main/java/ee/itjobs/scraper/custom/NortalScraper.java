package ee.itjobs.scraper.custom;

import com.fasterxml.jackson.databind.JsonNode;
import ee.itjobs.entity.Job;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
public class NortalScraper extends BaseScraper {

    public NortalScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "Nortal"; }

    @Override
    public String getSource() { return "nortal"; }

    @Override
    protected List<Job> scrape() throws Exception {
        Set<String> seenTitles = new HashSet<>();
        List<Job> allJobs = new ArrayList<>();

        // Source 1: iCIMS
        try {
            JsonNode icims = fetchJson("https://careers.nortal.com/hs/serverless/career");
            if (icims.isArray()) {
                for (JsonNode node : icims) {
                    String title = node.path("title").asText("");
                    String location = node.path("location").asText("");
                    if (!containsEstoniaLocation(location)) continue;
                    if (seenTitles.contains(title.toLowerCase())) continue;
                    seenTitles.add(title.toLowerCase());

                    String jobUrl = node.path("url").asText("");
                    allJobs.add(buildJob(title, "Nortal", location, jobUrl, null, null, null));
                }
            }
        } catch (Exception e) {
            log.warn("[Nortal] iCIMS source failed: {}", e.getMessage());
        }

        // Source 2: Greenhouse
        try {
            JsonNode gh = fetchJson("https://careers.nortal.com/_hcms/api/greenhouse");
            if (gh.isArray()) {
                for (JsonNode node : gh) {
                    String title = node.path("title").asText("");
                    String location = node.path("location").path("name").asText("");
                    if (!containsEstoniaLocation(location)) continue;
                    if (seenTitles.contains(title.toLowerCase())) continue;
                    seenTitles.add(title.toLowerCase());

                    String jobUrl = node.path("absolute_url").asText("");
                    allJobs.add(buildJob(title, "Nortal", location, jobUrl, null, null, null));
                }
            }
        } catch (Exception e) {
            log.warn("[Nortal] Greenhouse source failed: {}", e.getMessage());
        }

        return allJobs;
    }
}
