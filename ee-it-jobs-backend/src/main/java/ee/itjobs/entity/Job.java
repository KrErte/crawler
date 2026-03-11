package ee.itjobs.entity;

import ee.itjobs.enums.JobType;
import ee.itjobs.enums.WorkplaceType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String source;

    @Column(name = "date_posted")
    private LocalDate datePosted;

    @Column(name = "date_scraped")
    private LocalDate dateScraped;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    @Builder.Default
    private JobType jobType = JobType.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "workplace_type")
    @Builder.Default
    private WorkplaceType workplaceType = WorkplaceType.UNKNOWN;

    private String department;

    @Column(name = "salary_text")
    private String salaryText;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    @Column(name = "description_snippet", columnDefinition = "TEXT")
    private String descriptionSnippet;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> skills;

    @Column(name = "dedup_key")
    private String dedupKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrape_run_id")
    private ScrapeRun scrapeRun;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
