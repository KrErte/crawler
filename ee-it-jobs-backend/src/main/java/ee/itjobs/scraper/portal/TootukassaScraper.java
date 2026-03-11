package ee.itjobs.scraper.portal;

import com.fasterxml.jackson.databind.JsonNode;
import ee.itjobs.entity.Job;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TootukassaScraper extends BaseScraper {

    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 8;

    public TootukassaScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "Töötukassa"; }

    @Override
    public String getSource() { return "tootukassa"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            try {
                // Töötukassa public API - IT sector (ISCO code 25 = ICT professionals)
                String url = "https://www.tootukassa.ee/api/public/vacancies?isco=25&offset=" + (page * PAGE_SIZE) + "&limit=" + PAGE_SIZE;
                JsonNode root = fetchJson(url);
                JsonNode vacancies = root.isArray() ? root : root.path("data");

                if (!vacancies.isArray() || vacancies.isEmpty()) break;

                for (JsonNode v : vacancies) {
                    String title = v.path("profession").asText(v.path("title").asText(""));
                    String company = v.path("employer").asText(v.path("companyName").asText(""));
                    String location = v.path("location").asText(v.path("address").asText("Estonia"));
                    String id = v.path("id").asText("");
                    String jobUrl = "https://www.tootukassa.ee/toopakkumised/" + id;
                    String description = v.path("description").asText("");

                    String salaryText = null;
                    if (v.has("salaryFrom") || v.has("salaryTo")) {
                        int from = v.path("salaryFrom").asInt(0);
                        int to = v.path("salaryTo").asInt(0);
                        if (from > 0 && to > 0) {
                            salaryText = from + "-" + to + " EUR";
                        } else if (from > 0) {
                            salaryText = "from " + from + " EUR";
                        } else if (to > 0) {
                            salaryText = "up to " + to + " EUR";
                        }
                    }

                    if (!title.isEmpty()) {
                        allJobs.add(buildJob(title, company, location, jobUrl, description, salaryText, null));
                    }
                }

                if (vacancies.size() < PAGE_SIZE) break;
            } catch (Exception e) {
                log.warn("[Töötukassa] Page {} failed: {}", page, e.getMessage());
                break;
            }
        }
        return allJobs;
    }
}
