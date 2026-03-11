package ee.itjobs.repository;

import ee.itjobs.entity.ScrapeRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
    Optional<ScrapeRun> findTopByOrderByStartedAtDesc();
    Optional<ScrapeRun> findByStatus(String status);
    Page<ScrapeRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
