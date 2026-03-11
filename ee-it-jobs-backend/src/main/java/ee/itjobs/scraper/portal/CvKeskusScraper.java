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
public class CvKeskusScraper extends BaseScraper {

    private static final int PAGE_SIZE = 25;
    private static final int MAX_OFFSET = 400;

    public CvKeskusScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "CVKeskus"; }

    @Override
    public String getSource() { return "cvkeskus"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();

        for (int offset = 0; offset < MAX_OFFSET; offset += PAGE_SIZE) {
            String url = "https://www.cvkeskus.ee/toopakkumised-infotehnoloogia-valdkonnas?start=" + offset;
            String html = fetchPage(url);
            Document doc = parseHtml(html);

            Elements cards = doc.select("a.jobad-url");
            if (cards.isEmpty()) break;

            for (Element card : cards) {
                String title = "";
                Element h2 = card.selectFirst("h2");
                if (h2 != null) title = h2.text().trim();

                // Normalize ALL-CAPS to title case
                if (title.equals(title.toUpperCase()) && !title.isEmpty()) {
                    title = toTitleCase(title);
                }

                String company = "";
                Element companyEl = card.selectFirst(".job-company");
                if (companyEl != null) company = companyEl.text().trim();

                String location = "";
                Element locEl = card.selectFirst("span.location");
                if (locEl != null) {
                    Element parent = locEl.parent();
                    location = parent != null ? parent.text().trim() : locEl.text().trim();
                }

                String salary = null;
                Element salaryEl = card.selectFirst("div.salary-block");
                if (salaryEl != null) salary = salaryEl.text().trim();

                String href = card.attr("href");
                if (!href.startsWith("http")) {
                    href = "https://www.cvkeskus.ee" + href;
                }

                allJobs.add(buildJob(title, company, location, href, null, salary, null));
            }
        }
        return allJobs;
    }

    private String toTitleCase(String text) {
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!result.isEmpty()) result.append(' ');
            if (word.length() <= 1) {
                result.append(word.toLowerCase());
            } else {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
