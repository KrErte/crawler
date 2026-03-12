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
public class AshbyScraper extends BaseScraper {

    private final String boardName;
    private final String companyName;

    public AshbyScraper(WebClient webClient, RateLimiter rateLimiter,
                         String boardName, String companyName) {
        super(webClient, rateLimiter);
        this.boardName = boardName;
        this.companyName = companyName;
    }

    @Override
    public String getName() { return "Ashby-" + companyName; }

    @Override
    public String getSource() { return "ashby"; }

    @Override
    protected List<Job> scrape() throws Exception {
        String url = "https://api.ashbyhq.com/posting-api/job-board/" + boardName;
        JsonNode root = fetchJson(url);
        List<Job> jobs = new ArrayList<>();

        JsonNode jobsNode = root.path("jobs");
        if (jobsNode.isArray()) {
            for (JsonNode node : jobsNode) {
                if (!node.path("isListed").asBoolean(false)) continue;

                String location = node.path("location").asText("");
                boolean estoniaFound = containsEstoniaLocation(location);

                if (!estoniaFound) {
                    JsonNode secondary = node.path("secondaryLocations");
                    if (secondary.isArray()) {
                        for (JsonNode loc : secondary) {
                            if (containsEstoniaLocation(loc.path("location").asText(""))) {
                                location = loc.path("location").asText("");
                                estoniaFound = true;
                                break;
                            }
                        }
                    }
                }

                if (!estoniaFound && !location.isEmpty()) continue;

                String title = node.path("title").asText("");
                String jobUrl = node.path("jobUrl").asText("");
                String department = node.path("department").asText("");
                String team = node.path("team").asText("");
                String description = node.path("descriptionPlain").asText("");
                String wpType = node.path("workplaceType").asText("");

                if (department.isEmpty() && !team.isEmpty()) {
                    department = team;
                }

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
