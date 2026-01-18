package hanyang.erica.domain.club.repository;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubApplicationRepository extends JpaRepository<ClubApplication, Long> {

    List<ClubApplication> findByStatus(VerificationStatus status);

    boolean existsByClubName(String clubName);
}
