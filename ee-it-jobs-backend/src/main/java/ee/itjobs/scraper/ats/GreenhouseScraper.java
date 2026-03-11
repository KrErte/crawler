package ee.itjobs.scraper.ats;

import com.fasterxml.jackson.databind.JsonNode;
import ee.itjobs.entity.Job;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GreenhouseScraper extends BaseScraper {

    private final String boardToken;
    private final String companyName;

    public GreenhouseScraper(WebClient webClient, RateLimiter rateLimiter,
                              String boardToken, String companyName) {
        super(webClient, rateLimiter);
        this.boardToken = boardToken;
        this.companyName = companyName;
    }

    @Override
    public String getName() { return "Greenhouse-" + companyName; }

    @Override
    public String getSource() { return "greenhouse"; }

    @Override
    protected List<Job> scrape() throws Exception {
        String url = "https://boards-api.greenhouse.io/v1/boards/" + boardToken + "/jobs";
        JsonNode root = fetchJson(url);
        List<Job> jobs = new ArrayList<>();

        JsonNode jobsNode = root.path("jobs");
        if (jobsNode.isArray()) {
            for (JsonNode node : jobsNode) {
                String location = node.path("location").path("name").asText("");
                if (!containsEstoniaLocation(location) && !location.isEmpty()) continue;

                String title = node.path("title").asText("");
                String jobUrl = node.path("absolute_url").asText("");
                String department = "";
                JsonNode departments = node.path("departments");
                if (departments.isArray() && !departments.isEmpty()) {
                    department = departments.get(0).path("name").asText("");
                }

                jobs.add(buildJob(title, companyName, location, jobUrl, null, null, department));
            }
        }
        return jobs;
    }
}
