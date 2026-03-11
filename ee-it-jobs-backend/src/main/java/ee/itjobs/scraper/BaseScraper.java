package ee.itjobs.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.itjobs.entity.Job;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseScraper {

    protected final WebClient webClient;
    protected final RateLimiter rateLimiter;
    protected final ObjectMapper objectMapper;

    public abstract String getName();
    public abstract String getSource();

    public BaseScraper(WebClient webClient, RateLimiter rateLimiter) {
        this.webClient = webClient;
        this.rateLimiter = rateLimiter;
        this.objectMapper = new ObjectMapper();
    }

    public ScrapeResult run() {
        long start = System.currentTimeMillis();
        ScrapeResult result = ScrapeResult.builder()
                .source(getSource())
                .build();
        try {
            List<Job> jobs = scrape();
            result.setJobs(jobs);
            log.info("[{}] Scraped {} jobs", getName(), jobs.size());
        } catch (Exception e) {
            log.error("[{}] Scrape failed: {}", getName(), e.getMessage(), e);
            result.getErrors().add(e.getMessage());
        }
        result.setDurationSeconds((System.currentTimeMillis() - start) / 1000.0);
        return result;
    }

    protected abstract List<Job> scrape() throws Exception;

    protected String fetchPage(String url) {
        rateLimiter.acquire();
        log.debug("[{}] Fetching {}", getName(), url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    protected JsonNode fetchJson(String url) {
        rateLimiter.acquire();
        log.debug("[{}] Fetching JSON {}", getName(), url);
        String body = webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[{}] Failed to parse JSON from {}", getName(), url, e);
            return objectMapper.createObjectNode();
        }
    }

    protected Document parseHtml(String html) {
        return Jsoup.parse(html);
    }

    protected Job buildJob(String title, String company, String location, String url,
                           String description, String salary, String department) {
        String dedupKey = (company + "|" + title + "|" + (location != null ? location : ""))
                .toLowerCase().trim();
        Job.JobBuilder builder = Job.builder()
                .title(title)
                .company(company)
                .location(location)
                .url(url)
                .source(getSource())
                .dateScraped(LocalDate.now())
                .descriptionSnippet(description != null && description.length() > 200
                        ? description.substring(0, 200) : description)
                .fullDescription(description)
                .salaryText(salary)
                .department(department)
                .dedupKey(dedupKey);

        // Parse salary text into structured fields
        String salarySource = salary != null ? salary : description;
        SalaryParser.ParsedSalary parsed = SalaryParser.parse(salarySource);
        if (parsed != null) {
            builder.salaryMin(parsed.min());
            builder.salaryMax(parsed.max());
            builder.salaryCurrency(parsed.currency());
        }

        return builder.build();
    }

    protected boolean containsEstoniaLocation(String location) {
        if (location == null) return false;
        String lower = location.toLowerCase();
        return lower.contains("estonia") || lower.contains("tallinn") || lower.contains("tartu")
                || lower.contains("pärnu") || lower.contains("narva") || lower.contains("eesti");
    }
}
