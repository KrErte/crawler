package ee.itjobs.scraper;

import ee.itjobs.entity.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeResult {
    private String source;
    @Builder.Default
    private List<Job> jobs = new ArrayList<>();
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    private double durationSeconds;
    private int pagesScraped;
}
