package hanyang.erica.domain.evidence.repository;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.evidence.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceCardRepository extends JpaRepository<EvidenceCard, Long> {

    List<EvidenceCard> findByClubApplication(ClubApplication clubApplication);

    List<EvidenceCard> findByClubApplicationAndSourceType(ClubApplication clubApplication, SourceType sourceType);

    int countByClubApplication(ClubApplication clubApplication);
}
