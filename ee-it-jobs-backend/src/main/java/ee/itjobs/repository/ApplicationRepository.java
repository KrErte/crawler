package ee.itjobs.repository;

import ee.itjobs.entity.Application;
import ee.itjobs.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserIdOrderByAppliedAtDesc(Long userId);
    List<Application> findByUserIdAndStatusOrderByAppliedAtDesc(Long userId, ApplicationStatus status);
    Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
    long countByUserId(Long userId);
}
