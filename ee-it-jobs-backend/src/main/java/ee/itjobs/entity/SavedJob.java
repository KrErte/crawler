package ee.itjobs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_jobs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "job_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "saved_at", nullable = false)
    @Builder.Default
    private LocalDateTime savedAt = LocalDateTime.now();
}
