package ee.itjobs.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "user_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "cv_file_path")
    private String cvFilePath;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> skills;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> preferences;

    @Column(name = "cv_raw_text", columnDefinition = "TEXT")
    private String cvRawText;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "role_level")
    private String roleLevel;

    @Column(name = "email_alerts")
    @Builder.Default
    private Boolean emailAlerts = false;

    @Column(name = "alert_threshold")
    @Builder.Default
    private Integer alertThreshold = 70;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    @Type(JsonType.class)
    @Column(name = "notification_preferences", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> notificationPreferences = new java.util.HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
