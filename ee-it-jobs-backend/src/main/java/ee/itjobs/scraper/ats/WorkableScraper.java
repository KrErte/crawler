package ee.itjobs.scraper.ats;

import com.fasterxml.jackson.databind.JsonNode;
import ee.itjobs.entity.Job;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WorkableScraper extends BaseScraper {

    private final String accountSlug;
    private final String companyName;

    public WorkableScraper(WebClient webClient, RateLimiter rateLimiter,
                            String accountSlug, String companyName) {
        super(webClient, rateLimiter);
        this.accountSlug = accountSlug;
        this.companyName = companyName;
    }

    @Override
    public String getName() { return "Workable-" + companyName; }

    @Override
    public String getSource() { return "workable"; }

    @Override
    protected List<Job> scrape() throws Exception {
        String url = "https://apply.workable.com/api/v1/widget/accounts/" + accountSlug;
        JsonNode root = fetchJson(url);
        List<Job> jobs = new ArrayList<>();

        JsonNode jobsNode = root.path("jobs");
        if (jobsNode.isArray()) {
            for (JsonNode node : jobsNode) {
                String city = node.path("city").asText("");
                String country = node.path("country").asText("");
                String location = city + ", " + country;

                if (!containsEstoniaLocation(location)) continue;

                String title = node.path("title").asText("");
                String jobUrl = node.path("url").asText("");
                String department = node.path("department").asText("");
                String publishedOn = node.path("published_on").asText("");

                Job job = buildJob(title, companyName, location, jobUrl, null, null, department);
                if (!publishedOn.isEmpty()) {
                    try {
                        job.setDatePosted(LocalDate.parse(publishedOn, DateTimeFormatter.ISO_DATE));
                    } catch (Exception ignored) {}
                }
                jobs.add(job);
            }
        }
        return jobs;
    }
}
