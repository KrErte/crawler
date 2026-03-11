package ee.itjobs.scraper.ats;

import ee.itjobs.entity.Job;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
public class TeamtailorScraper extends BaseScraper {

    private final String baseUrl;
    private final String companyName;
    private static final Set<String> SKIP_TEXTS = Set.of(
            "all jobs", "view all jobs", "see all jobs", "jobs");

    public TeamtailorScraper(WebClient webClient, RateLimiter rateLimiter,
                              String baseUrl, String companyName) {
        super(webClient, rateLimiter);
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.companyName = companyName;
    }

    @Override
    public String getName() { return "Teamtailor-" + companyName; }

    @Override
    public String getSource() { return "teamtailor"; }

    @Override
    protected List<Job> scrape() throws Exception {
        Set<String> seenUrls = new HashSet<>();
        List<Job> allJobs = new ArrayList<>();

        // First page
        allJobs.addAll(parsePage(baseUrl + "/jobs", seenUrls));

        // Paginated pages
        for (int page = 2; page <= 10; page++) {
            String url = baseUrl + "/jobs/show_more?page=" + page;
            try {
                List<Job> pageJobs = parsePage(url, seenUrls);
                if (pageJobs.isEmpty()) break;
                allJobs.addAll(pageJobs);
            } catch (Exception e) {
                break;
            }
        }
        return allJobs;
    }

    private List<Job> parsePage(String url, Set<String> seenUrls) {
        String html = fetchPage(url);
        Document doc = parseHtml(html);
        List<Job> jobs = new ArrayList<>();

        Elements links = doc.select("li a[href*='/jobs/'], a.focus-visible-company[href*='/jobs/']");
        if (links.isEmpty()) {
            links = doc.select("a[href*='/jobs/']");
        }

        for (Element link : links) {
            String text = link.text().trim();
            if (text.length() < 5 || SKIP_TEXTS.contains(text.toLowerCase())) continue;

            String href = link.absUrl("href");
            if (href.isEmpty()) href = baseUrl + link.attr("href");
            if (seenUrls.contains(href)) continue;
            seenUrls.add(href);

            // Extract title
            String title = text;
            Element titleEl = link.selectFirst("span, h3, h4, div[class*='title']");
            if (titleEl != null && !titleEl.text().isBlank()) {
                title = titleEl.text().trim();
            }

            // Extract location
            String location = "";
            Element locEl = link.selectFirst("[class*='location'], [class*='meta']");
            if (locEl != null) {
                location = locEl.text().trim();
            }

            if (!location.isEmpty() && !containsEstoniaLocation(location)) continue;

            jobs.add(buildJob(title, companyName, location, href, null, null, null));
        }
        return jobs;
    }
}
