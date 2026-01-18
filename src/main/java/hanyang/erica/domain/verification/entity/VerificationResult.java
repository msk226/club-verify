package hanyang.erica.domain.verification.entity;

import hanyang.erica.domain.club.entity.ClubApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "verification_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_application_id", nullable = false, unique = true)
    private ClubApplication clubApplication;

    @Column(nullable = false)
    private Integer totalRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(columnDefinition = "JSON")
    private String ruleBasedScoreBreakdown;

    @Column(columnDefinition = "TEXT")
    private String llmOverallObservation;

    @OneToMany(mappedBy = "verificationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnomalySignal> anomalySignals = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addAnomalySignal(AnomalySignal signal) {
        anomalySignals.add(signal);
    }

    public void updateLlmObservation(String observation) {
        this.llmOverallObservation = observation;
    }
}
