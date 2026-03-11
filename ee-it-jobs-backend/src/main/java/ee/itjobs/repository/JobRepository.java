package ee.itjobs.repository;

import ee.itjobs.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    Optional<Job> findByDedupKey(String dedupKey);

    @Query("SELECT DISTINCT j.company FROM Job j WHERE j.isActive = true ORDER BY j.company")
    List<String> findDistinctCompanies();

    @Query("SELECT DISTINCT j.source FROM Job j WHERE j.isActive = true ORDER BY j.source")
    List<String> findDistinctSources();

    List<Job> findByIsActiveTrue();

    List<Job> findByIsActiveTrueOrderByDateScrapedDesc();

    long countByIsActiveTrue();

    @Query("SELECT DISTINCT j.company FROM Job j WHERE j.isActive = true AND LOWER(j.company) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY j.company")
    List<String> findCompanySuggestions(@Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT j.title FROM Job j WHERE j.isActive = true AND LOWER(j.title) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY j.title")
    List<String> findTitleSuggestions(@Param("q") String q, Pageable pageable);
}
