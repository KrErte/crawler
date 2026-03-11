package ee.itjobs.scraper.portal;

import com.fasterxml.jackson.databind.JsonNode;
import ee.itjobs.entity.Job;
import ee.itjobs.enums.WorkplaceType;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MeetFrankScraper extends BaseScraper {

    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 10;

    public MeetFrankScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "MeetFrank"; }

    @Override
    public String getSource() { return "meetfrank"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            try {
                String url = "https://api.meetfrank.com/v1/public/positions?country=EE&category=ENGINEERING&offset=" + (page * PAGE_SIZE) + "&limit=" + PAGE_SIZE;
                JsonNode root = fetchJson(url);
                JsonNode positions = root.path("data");

                if (!positions.isArray() || positions.isEmpty()) break;

                for (JsonNode pos : positions) {
                    String title = pos.path("title").asText("");
                    String company = pos.path("company").path("name").asText("");
                    String location = pos.path("location").path("city").asText("Estonia");
                    String jobUrl = "https://meetfrank.com/job/" + pos.path("slug").asText(pos.path("id").asText(""));
                    String description = pos.path("description").asText("");

                    String salaryText = null;
                    JsonNode salary = pos.path("salary");
                    if (!salary.isMissingNode() && salary.has("min")) {
                        int min = salary.path("min").asInt(0);
                        int max = salary.path("max").asInt(0);
                        String currency = salary.path("currency").asText("EUR");
                        if (min > 0 || max > 0) {
                            salaryText = min + "-" + max + " " + currency;
                        }
                    }

                    String wpType = pos.path("workType").asText("");

                    Job job = buildJob(title, company, location, jobUrl, description, salaryText, null);
                    job.setWorkplaceType(mapWorkplaceType(wpType));
                    allJobs.add(job);
                }

                if (positions.size() < PAGE_SIZE) break;
            } catch (Exception e) {
                log.warn("[MeetFrank] Page {} failed: {}", page, e.getMessage());
                break;
            }
        }
        return allJobs;
    }

    private WorkplaceType mapWorkplaceType(String type) {
        if (type == null) return WorkplaceType.UNKNOWN;
        return switch (type.toUpperCase()) {
            case "REMOTE" -> WorkplaceType.REMOTE;
            case "HYBRID" -> WorkplaceType.HYBRID;
            case "ON_SITE", "ONSITE", "OFFICE" -> WorkplaceType.ONSITE;
            default -> WorkplaceType.UNKNOWN;
        };
    }
}
