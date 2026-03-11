package ee.itjobs.scraper.portal;

import com.fasterxml.jackson.databind.JsonNode;
import ee.itjobs.entity.Job;
import ee.itjobs.enums.WorkplaceType;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;

@Slf4j
public class CvEeScraper extends BaseScraper {

    private static final Map<Integer, String> TOWN_ID_MAP = Map.ofEntries(
            Map.entry(312, "Tallinn"), Map.entry(314, "Tartu"), Map.entry(321, "Pärnu"),
            Map.entry(318, "Narva"), Map.entry(313, "Haapsalu"), Map.entry(315, "Viljandi"),
            Map.entry(316, "Rakvere"), Map.entry(320, "Kuressaare"), Map.entry(319, "Jõhvi"),
            Map.entry(317, "Paide"), Map.entry(322, "Valga"), Map.entry(323, "Võru")
    );

    public CvEeScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "CV.ee"; }

    @Override
    public String getSource() { return "cv.ee"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();
        int offset = 0;
        int limit = 25;

        while (offset < 400) {
            String url = "https://www.cv.ee/et/search?categories%5B0%5D=INFORMATION_TECHNOLOGY&limit=" + limit + "&offset=" + offset;
            String html = fetchPage(url);
            Document doc = parseHtml(html);

            Element scriptEl = doc.selectFirst("script#__NEXT_DATA__");
            if (scriptEl == null) break;

            JsonNode root = objectMapper.readTree(scriptEl.data());
            JsonNode vacancies = root.path("props").path("pageProps").path("searchResults").path("vacancies");

            if (!vacancies.isArray() || vacancies.isEmpty()) break;

            for (JsonNode v : vacancies) {
                String title = v.path("positionTitle").asText("");
                String company = v.path("employerName").asText("");
                int townId = v.path("townId").asInt(0);
                String location = TOWN_ID_MAP.getOrDefault(townId, "Estonia");
                String remoteType = v.path("remoteWorkType").asText("");
                String description = v.path("positionContent").asText("");
                String salary = v.path("salaryText").asText("");
                String id = v.path("id").asText("");
                String publishDate = v.path("publishDate").asText("");
                String jobUrl = "https://www.cv.ee/et/vacancy/" + id;

                Job job = buildJob(title, company, location, jobUrl,
                        description.length() > 200 ? description.substring(0, 200) : description,
                        salary.isEmpty() ? null : salary, null);
                job.setFullDescription(description);
                job.setWorkplaceType(mapRemoteType(remoteType));
                if (!publishDate.isEmpty()) {
                    try {
                        job.setDatePosted(LocalDate.parse(publishDate.substring(0, 10)));
                    } catch (Exception ignored) {}
                }
                allJobs.add(job);
            }
            offset += limit;
        }
        return allJobs;
    }

    private WorkplaceType mapRemoteType(String type) {
        if (type == null) return WorkplaceType.UNKNOWN;
        return switch (type) {
            case "FULLY_REMOTE" -> WorkplaceType.REMOTE;
            case "HYBRID" -> WorkplaceType.HYBRID;
            case "ON_SITE" -> WorkplaceType.ONSITE;
            default -> WorkplaceType.UNKNOWN;
        };
    }
}
