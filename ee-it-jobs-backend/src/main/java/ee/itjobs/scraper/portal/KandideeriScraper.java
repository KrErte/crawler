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
public class KandideeriScraper extends BaseScraper {

    private static final int MAX_PAGES = 10;

    public KandideeriScraper(WebClient webClient, RateLimiter rateLimiter) {
        super(webClient, rateLimiter);
    }

    @Override
    public String getName() { return "Kandideeri"; }

    @Override
    public String getSource() { return "kandideeri"; }

    @Override
    protected List<Job> scrape() throws Exception {
        List<Job> allJobs = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = "https://www.kandideeri.ee/categories/IT-toopakkumised/tarkvara-arendus/?page=" + page;
            String html = fetchPage(url);
            Document doc = parseHtml(html);

            Elements cards = doc.select("article.listing-item");
            if (cards.isEmpty()) break;

            for (Element card : cards) {
                String title = "";
                String href = "";
                Element titleLink = card.selectFirst(".listing-item__title a.link");
                if (titleLink != null) {
                    title = titleLink.text().trim();
                    href = titleLink.absUrl("href");
                    if (href.isEmpty()) {
                        href = "https://www.kandideeri.ee" + titleLink.attr("href");
                    }
                }

                String company = "";
                Element companyEl = card.selectFirst(".listing-item__additional--company");
                if (companyEl != null) company = companyEl.text().trim();

                String location = "";
                Element locEl = card.selectFirst(".listing-item__additional--location");
                if (locEl != null) location = locEl.text().trim();

                if (!title.isEmpty() && !href.isEmpty()) {
                    allJobs.add(buildJob(title, company, location, href, null, null, null));
                }
            }
        }
        return allJobs;
    }
}
