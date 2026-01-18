package hanyang.erica.domain.verification.repository;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.verification.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {

    Optional<VerificationResult> findByClubApplication(ClubApplication clubApplication);

    Optional<VerificationResult> findByClubApplicationId(Long clubApplicationId);
}
