package hanyang.erica.domain.verification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.VerificationStatus;
import hanyang.erica.domain.club.repository.ClubApplicationRepository;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.evidence.service.EvidenceService;
import hanyang.erica.domain.verification.entity.AnomalySignal;
import hanyang.erica.domain.verification.entity.RiskLevel;
import hanyang.erica.domain.verification.entity.VerificationResult;
import hanyang.erica.domain.verification.repository.VerificationResultRepository;
import hanyang.erica.infrastructure.llm.LlmClient;
import hanyang.erica.infrastructure.llm.dto.DeepAnalysisResult;
import hanyang.erica.infrastructure.llm.dto.LlmRequest;
import hanyang.erica.infrastructure.llm.dto.LlmResponse;
import hanyang.erica.infrastructure.llm.prompt.DeepAnalysisPrompt;
import hanyang.erica.infrastructure.notification.SlackNotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationOrchestrator {

    private final ClubApplicationRepository clubApplicationRepository;
    private final EvidenceService evidenceService;
    private final RiskScoreCalculator riskScoreCalculator;
    private final LlmClient llmClient;
    private final VerificationResultRepository verificationResultRepository;
    private final SlackNotificationClient slackNotificationClient;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");

    @Async("verificationExecutor")
    @Transactional
    public void startVerification(Long applicationId) {
        log.info("Starting verification process for application: {}", applicationId);

        ClubApplication application = clubApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        try {
            application.updateStatus(VerificationStatus.VERIFICATION_IN_PROGRESS);
            clubApplicationRepository.save(application);

            List<EvidenceCard> evidenceCards = evidenceService.collectAndAnalyzeEvidence(application).block();

            RiskScoreCalculator.RiskScoreResult scoreResult = riskScoreCalculator.calculate(application, evidenceCards);

            VerificationResult verificationResult = createVerificationResult(application, scoreResult);

            performDeepAnalysis(application, evidenceCards, scoreResult, verificationResult);

            updateApplicationStatus(application, scoreResult.riskLevel());

            slackNotificationClient.sendVerificationResult(application, verificationResult, evidenceCards);

            log.info("Verification completed for application: {}, riskLevel: {}",
                    applicationId, scoreResult.riskLevel());

        } catch (Exception e) {
            log.error("Verification failed for application: {}", applicationId, e);
            application.updateStatus(VerificationStatus.MANUAL_REVIEW_REQUIRED);
            clubApplicationRepository.save(application);
            slackNotificationClient.sendVerificationError(application, e.getMessage());
        }
    }

    private VerificationResult createVerificationResult(
            ClubApplication application,
            RiskScoreCalculator.RiskScoreResult scoreResult) {

        String breakdownJson;
        try {
            breakdownJson = objectMapper.writeValueAsString(scoreResult.breakdown());
        } catch (JsonProcessingException e) {
            breakdownJson = "{}";
        }

        VerificationResult result = VerificationResult.builder()
                .clubApplication(application)
                .totalRiskScore(scoreResult.totalScore())
                .riskLevel(scoreResult.riskLevel())
                .ruleBasedScoreBreakdown(breakdownJson)
                .build();

        return verificationResultRepository.save(result);
    }

    private void performDeepAnalysis(
            ClubApplication application,
            List<EvidenceCard> evidenceCards,
            RiskScoreCalculator.RiskScoreResult scoreResult,
            VerificationResult verificationResult) {

        log.info("Performing deep analysis for application: {}", application.getId());

        String systemPrompt = DeepAnalysisPrompt.getSystemPrompt();
        String userPrompt = DeepAnalysisPrompt.buildUserPrompt(
                application,
                evidenceCards,
                scoreResult.totalScore(),
                scoreResult.riskLevel(),
                scoreResult.breakdown()
        );

        LlmRequest request = LlmRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .maxTokens(4096)
                .temperature(0.3)
                .build();

        try {
            LlmResponse response = llmClient.chat(request).block();

            if (response != null && response.isSuccess()) {
                DeepAnalysisResult analysisResult = parseDeepAnalysisResult(response);
                applyDeepAnalysisResult(verificationResult, analysisResult);
            } else {
                log.warn("Deep analysis LLM call was not successful");
                verificationResult.updateLlmObservation("LLM 심층 분석 실패 - 수동 검토 필요");
            }
        } catch (Exception e) {
            log.error("Deep analysis failed", e);
            verificationResult.updateLlmObservation("LLM 심층 분석 중 오류 발생: " + e.getMessage());
        }

        verificationResultRepository.save(verificationResult);
    }

    private DeepAnalysisResult parseDeepAnalysisResult(LlmResponse response) {
        String content = response.getContent();
        String jsonContent = extractJson(content);

        try {
            return objectMapper.readValue(jsonContent, DeepAnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse deep analysis result: {}", e.getMessage());
            return DeepAnalysisResult.builder()
                    .overallObservation(content)
                    .recommendation("파싱 실패 - 원문 확인 필요")
                    .confidenceScore(0)
                    .build();
        }
    }

    private String extractJson(String content) {
        Matcher matcher = JSON_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content.trim();
    }

    private void applyDeepAnalysisResult(VerificationResult verificationResult, DeepAnalysisResult analysisResult) {
        verificationResult.updateLlmObservation(analysisResult.getOverallObservation());

        if (analysisResult.getAnomalies() != null) {
            for (var anomaly : analysisResult.getAnomalies()) {
                AnomalySignal signal = AnomalySignal.builder()
                        .verificationResult(verificationResult)
                        .signalType(anomaly.getSignalType())
                        .description(anomaly.getDescription())
                        .severity(anomaly.getSeverity())
                        .evidence(anomaly.getEvidence())
                        .build();
                verificationResult.addAnomalySignal(signal);
            }
        }
    }

    private void updateApplicationStatus(ClubApplication application, RiskLevel riskLevel) {
        VerificationStatus newStatus = switch (riskLevel) {
            case AUTO_APPROVE -> VerificationStatus.AUTO_APPROVED;
            case PENDING_REVIEW -> VerificationStatus.PENDING_REVIEW;
            case MANUAL_REVIEW -> VerificationStatus.MANUAL_REVIEW_REQUIRED;
        };

        application.updateStatus(newStatus);
        clubApplicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public VerificationResult getVerificationResult(Long applicationId) {
        return verificationResultRepository.findByClubApplicationId(applicationId)
                .orElse(null);
    }
}
