package ee.itjobs.scraper.custom;

import ee.itjobs.entity.Job;
import ee.itjobs.scraper.BaseScraper;
import ee.itjobs.scraper.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class HelmesScraper extends BaseScraper {

    private static final Set<String> SKIP_TEXTS = Set.of(
            "careers", "jobs", "open positions", "career", "join us");

    public HelmesScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "Helmes"; }

    @Override
    public String getSource() { return "helmes"; }

    @Override
    protected List<Job> scrape() throws Exception {
        String html = fetchPage("https://www.helmes.com/careers/");
        Document doc = parseHtml(html);
        List<Job> jobs = new ArrayList<>();

        Elements links = doc.select("a[href*='career'], a[href*='position'], a[href*='job']");
        for (Element link : links) {
            String title = link.text().trim();
            if (title.length() < 5) continue;
            if (SKIP_TEXTS.contains(title.toLowerCase())) continue;

            String href = link.absUrl("href");
            if (href.isEmpty()) href = "https://www.helmes.com" + link.attr("href");

            jobs.add(buildJob(title, "Helmes", "Tallinn, Estonia", href, null, null, null));
        }
        return jobs;
    }
}
