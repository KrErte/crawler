package ee.itjobs.scraper.ats;

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
public class LeverScraper extends BaseScraper {

    private final String companySlug;
    private final String companyName;
    private final String apiHost;

    public LeverScraper(WebClient webClient, RateLimiter rateLimiter,
                         String companySlug, String companyName, String apiHost) {
        super(webClient, rateLimiter);
        this.companySlug = companySlug;
        this.companyName = companyName;
        this.apiHost = apiHost != null ? apiHost : "api.lever.co";
    }

    @Override
    public String getName() { return "Lever-" + companyName; }

    @Override
    public String getSource() { return "lever"; }

    @Override
    protected List<Job> scrape() throws Exception {
        String url = "https://" + apiHost + "/v0/postings/" + companySlug + "?mode=json";
        JsonNode root = fetchJson(url);
        List<Job> jobs = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                String location = "";
                if (node.has("categories") && node.get("categories").has("location")) {
                    location = node.get("categories").get("location").asText("");
                }
                if (!containsEstoniaLocation(location) && !location.isEmpty()) continue;

                String title = node.path("text").asText("");
                String jobUrl = node.path("hostedUrl").asText("");
                String department = "";
                if (node.has("categories") && node.get("categories").has("department")) {
                    department = node.get("categories").get("department").asText("");
                }
                String description = node.path("descriptionPlain").asText("");
                String wpType = node.path("workplaceType").asText("");

                Job job = buildJob(title, companyName, location, jobUrl, description, null, department);
                job.setWorkplaceType(parseWorkplaceType(wpType));
                jobs.add(job);
            }
        }
        return jobs;
    }

    private WorkplaceType parseWorkplaceType(String type) {
        if (type == null) return WorkplaceType.UNKNOWN;
        String lower = type.toLowerCase();
        if (lower.contains("remote")) return WorkplaceType.REMOTE;
        if (lower.contains("hybrid")) return WorkplaceType.HYBRID;
        if (lower.contains("onsite") || lower.contains("on-site")) return WorkplaceType.ONSITE;
        return WorkplaceType.UNKNOWN;
    }
}
