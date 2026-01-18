package hanyang.erica.domain.verification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "anomaly_signals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnomalySignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_result_id", nullable = false)
    private VerificationResult verificationResult;

    @Column(nullable = false, length = 100)
    private String signalType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer severity;

    @Column(length = 500)
    private String evidence;
}
