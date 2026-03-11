package ee.itjobs.controller;

import ee.itjobs.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final JobRepository jobRepository;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        String[] pages = {"", "jobs", "stats", "login", "register"};
        for (String page : pages) {
            sb.append("  <url><loc>https://eeitjobs.ee/").append(page).append("</loc>");
            sb.append("<changefreq>daily</changefreq>");
            sb.append("<priority>").append(page.isEmpty() ? "1.0" : "0.8").append("</priority></url>\n");
        }

        // Dynamic job pages
        jobRepository.findByIsActiveTrue().forEach(job -> {
            sb.append("  <url><loc>https://eeitjobs.ee/jobs/").append(job.getId()).append("</loc>");
            sb.append("<lastmod>").append(job.getDateScraped()).append("</lastmod>");
            sb.append("<changefreq>weekly</changefreq>");
            sb.append("<priority>0.6</priority></url>\n");
        });

        sb.append("</urlset>");
        return sb.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /api/
                Disallow: /admin
                Disallow: /profile
                Sitemap: https://eeitjobs.ee/sitemap.xml
                """;
    }
}
