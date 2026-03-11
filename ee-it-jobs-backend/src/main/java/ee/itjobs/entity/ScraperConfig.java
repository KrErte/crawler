package ee.itjobs.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "scraper_configs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScraperConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scraper_type", nullable = false)
    private String scraperType;

    @Column(name = "company_name")
    private String companyName;

    @Type(JsonType.class)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private Map<String, String> configJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "requires_browser", nullable = false)
    @Builder.Default
    private Boolean requiresBrowser = false;
}
