package ee.itjobs.dto.job;

import ee.itjobs.enums.JobType;
import ee.itjobs.enums.WorkplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobDto {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String url;
    private String source;
    private LocalDate datePosted;
    private LocalDate dateScraped;
    private JobType jobType;
    private WorkplaceType workplaceType;
    private String department;
    private String salaryText;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
    private String descriptionSnippet;
    private String fullDescription;
    private List<String> skills;
}
