package ee.itjobs.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "scrape_runs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScrapeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private String status;

    @Column(name = "total_jobs")
    @Builder.Default
    private Integer totalJobs = 0;

    @Column(name = "total_new_jobs")
    @Builder.Default
    private Integer totalNewJobs = 0;

    @Column(name = "total_errors")
    @Builder.Default
    private Integer totalErrors = 0;

    @Type(JsonType.class)
    @Column(name = "source_stats", columnDefinition = "jsonb")
    private Map<String, Object> sourceStats;

    @Column(name = "triggered_by")
    private String triggeredBy;
}
