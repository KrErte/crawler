package ee.itjobs.repository;

import ee.itjobs.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    List<SavedJob> findByUserEmailOrderBySavedAtDesc(String email);
    Optional<SavedJob> findByUserEmailAndJobId(String email, Long jobId);
    boolean existsByUserEmailAndJobId(String email, Long jobId);
    void deleteByUserEmailAndJobId(String email, Long jobId);

    @Query("SELECT s.job.id FROM SavedJob s WHERE s.user.email = :email")
    List<Long> findJobIdsByUserEmail(String email);
}
