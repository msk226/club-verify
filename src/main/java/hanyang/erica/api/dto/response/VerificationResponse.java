package hanyang.erica.api.dto.response;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.VerificationStatus;
import hanyang.erica.domain.verification.entity.RiskLevel;
import hanyang.erica.domain.verification.entity.VerificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponse {

    private Long applicationId;
    private String clubName;
    private VerificationStatus status;
    private String statusDescription;
    private Integer totalRiskScore;
    private RiskLevel riskLevel;
    private String riskLevelDescription;
    private String scoreBreakdown;
    private String llmObservation;
    private List<AnomalySignalDto> anomalySignals;
    private LocalDateTime verifiedAt;
    private String message;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnomalySignalDto {
        private String signalType;
        private String description;
        private Integer severity;
        private String evidence;
    }

    public static VerificationResponse pending(ClubApplication application) {
        return VerificationResponse.builder()
                .applicationId(application.getId())
                .clubName(application.getClubName())
                .status(application.getStatus())
                .statusDescription(application.getStatus().getDescription())
                .message("검증이 시작되었습니다. 결과는 Slack으로 알림됩니다.")
                .build();
    }

    public static VerificationResponse fromResult(ClubApplication application, VerificationResult result) {
        List<AnomalySignalDto> anomalyDtos = null;
        if (result.getAnomalySignals() != null && !result.getAnomalySignals().isEmpty()) {
            anomalyDtos = result.getAnomalySignals().stream()
                    .map(signal -> AnomalySignalDto.builder()
                            .signalType(signal.getSignalType())
                            .description(signal.getDescription())
                            .severity(signal.getSeverity())
                            .evidence(signal.getEvidence())
                            .build())
                    .toList();
        }

        return VerificationResponse.builder()
                .applicationId(application.getId())
                .clubName(application.getClubName())
                .status(application.getStatus())
                .statusDescription(application.getStatus().getDescription())
                .totalRiskScore(result.getTotalRiskScore())
                .riskLevel(result.getRiskLevel())
                .riskLevelDescription(result.getRiskLevel().getDescription())
                .scoreBreakdown(result.getRuleBasedScoreBreakdown())
                .llmObservation(result.getLlmOverallObservation())
                .anomalySignals(anomalyDtos)
                .verifiedAt(result.getCreatedAt())
                .build();
    }

    public static VerificationResponse notFound(Long applicationId) {
        return VerificationResponse.builder()
                .applicationId(applicationId)
                .message("검증 결과를 찾을 수 없습니다. 아직 검증이 진행 중이거나 신청이 존재하지 않습니다.")
                .build();
    }
}
