package hanyang.erica.domain.evidence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvidenceSignal {

    @Column(name = "signal_strength")
    private Integer strength;

    @Column(name = "signal_type", length = 50)
    private String type;

    @Column(name = "risk_keywords", length = 500)
    private String riskKeywords;

    @Column(name = "is_official_source")
    private Boolean isOfficialSource;

    @Column(name = "activity_duration_months")
    private Integer activityDurationMonths;
}
