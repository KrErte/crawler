package ee.itjobs.repository;

import ee.itjobs.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<UserProfile> findByEmailAlertsTrue();
}
