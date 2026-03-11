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
public class SmartRecruitersScraper extends BaseScraper {

    private final String companyId;
    private final String companyName;
    private static final int PAGE_SIZE = 100;

    public SmartRecruitersScraper(WebClient webClient, RateLimiter rateLimiter,
                                   String companyId, String companyName) {
        super(webClient, rateLimiter);
        this.companyId = companyId;
        this.companyName = companyName;
    }

    @Override
    public String getName() { return "SmartRecruiters-" + companyName; }

    @Override
    public String getSource() { return "smartrecruiters"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();
        int offset = 0;

        while (true) {
            String url = "https://api.smartrecruiters.com/v1/companies/" + companyId +
                    "/postings?offset=" + offset + "&limit=" + PAGE_SIZE;
            JsonNode root = fetchJson(url);
            JsonNode content = root.path("content");

            if (!content.isArray() || content.isEmpty()) break;

            for (JsonNode node : content) {
                String city = node.path("location").path("city").asText("");
                String country = node.path("location").path("country").asText("");
                String location = city + ", " + country;

                String locLower = location.toLowerCase();
                if (!locLower.contains("tallinn") && !locLower.contains("tartu") && !locLower.contains("estoni")) {
                    continue;
                }

                String title = node.path("name").asText("");
                String jobUrl = node.path("ref").asText("");
                String department = node.path("department").path("label").asText("");

                allJobs.add(buildJob(title, companyName, location, jobUrl, null, null, department));
            }

            offset += PAGE_SIZE;
            if (content.size() < PAGE_SIZE) break;
        }
        return allJobs;
    }
}
