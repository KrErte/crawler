package ee.itjobs.scraper.portal;

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

@Slf4j
public class IndeedScraper extends BaseScraper {

    private static final int MAX_PAGES = 10;

    public IndeedScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "Indeed"; }

    @Override
    public String getSource() { return "indeed"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();

        for (int start = 0; start < MAX_PAGES * 10; start += 10) {
            try {
                String url = "https://ee.indeed.com/jobs?q=IT+developer+software&l=Estonia&start=" + start;
                String html = fetchPage(url);
                Document doc = parseHtml(html);

                Elements cards = doc.select("div.job_seen_beacon, div.cardOutline, td.resultContent");
                if (cards.isEmpty()) break;

                for (Element card : cards) {
                    String title = "";
                    Element titleEl = card.selectFirst("h2.jobTitle a, a[data-jk]");
                    if (titleEl != null) {
                        title = titleEl.text().trim();
                    }
                    if (title.isEmpty()) {
                        titleEl = card.selectFirst("h2.jobTitle span");
                        if (titleEl != null) title = titleEl.text().trim();
                    }

                    String company = "";
                    Element companyEl = card.selectFirst("span[data-testid='company-name'], span.companyName");
                    if (companyEl != null) company = companyEl.text().trim();

                    String location = "";
                    Element locEl = card.selectFirst("div[data-testid='text-location'], div.companyLocation");
                    if (locEl != null) location = locEl.text().trim();

                    String salary = null;
                    Element salaryEl = card.selectFirst("div[data-testid='attribute_snippet_testid'], div.salary-snippet-container, span.estimated-salary");
                    if (salaryEl != null) salary = salaryEl.text().trim();

                    String href = "";
                    Element linkEl = card.selectFirst("a[data-jk], h2.jobTitle a");
                    if (linkEl != null) {
                        String jk = linkEl.attr("data-jk");
                        if (!jk.isEmpty()) {
                            href = "https://ee.indeed.com/viewjob?jk=" + jk;
                        } else {
                            href = linkEl.absUrl("href");
                            if (href.isEmpty()) href = "https://ee.indeed.com" + linkEl.attr("href");
                        }
                    }

                    String snippet = "";
                    Element snippetEl = card.selectFirst("div.job-snippet, td.snip");
                    if (snippetEl != null) snippet = snippetEl.text().trim();

                    if (!title.isEmpty() && !href.isEmpty()) {
                        allJobs.add(buildJob(title, company, location, href, snippet, salary, null));
                    }
                }

                if (cards.size() < 10) break;
            } catch (Exception e) {
                log.warn("[Indeed] Page {} failed: {}", start / 10, e.getMessage());
                break;
            }
        }
        return allJobs;
    }
}
