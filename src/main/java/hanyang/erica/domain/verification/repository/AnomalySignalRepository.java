package hanyang.erica.domain.verification.repository;

import hanyang.erica.domain.verification.entity.AnomalySignal;
import hanyang.erica.domain.verification.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalySignalRepository extends JpaRepository<AnomalySignal, Long> {

    List<AnomalySignal> findByVerificationResult(VerificationResult verificationResult);
}
