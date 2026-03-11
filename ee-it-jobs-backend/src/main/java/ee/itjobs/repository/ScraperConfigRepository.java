package ee.itjobs.repository;

import ee.itjobs.entity.ScraperConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScraperConfigRepository extends JpaRepository<ScraperConfig, Long> {
    List<ScraperConfig> findByIsActiveTrue();
    List<ScraperConfig> findByScraperType(String scraperType);
}
