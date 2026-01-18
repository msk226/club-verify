package hanyang.erica.infrastructure.notification;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.verification.entity.RiskLevel;
import hanyang.erica.domain.verification.entity.VerificationResult;
import hanyang.erica.infrastructure.notification.dto.SlackMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class SlackNotificationClient {

    private final WebClient webClient;
    private final String webhookUrl;
    private final int timeout;

    public SlackNotificationClient(
            WebClient webClient,
            @Value("${external.slack.webhook-url:}") String webhookUrl,
            @Value("${external.slack.timeout:5000}") int timeout) {
        this.webClient = webClient;
        this.webhookUrl = webhookUrl;
        this.timeout = timeout;
    }

    public void sendVerificationResult(ClubApplication application, VerificationResult result, List<EvidenceCard> evidenceCards) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL not configured. Skipping notification.");
            return;
        }

        SlackMessage message = buildVerificationResultMessage(application, result, evidenceCards);
        sendMessage(message);
    }

    public void sendVerificationError(ClubApplication application, String errorMessage) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL not configured. Skipping notification.");
            return;
        }

        SlackMessage message = buildErrorMessage(application, errorMessage);
        sendMessage(message);
    }

    private SlackMessage buildVerificationResultMessage(ClubApplication application, VerificationResult result, List<EvidenceCard> evidenceCards) {
        String emoji = getEmojiForRiskLevel(result.getRiskLevel());
        String status = result.getRiskLevel().getDescription();

        StringBuilder text = new StringBuilder();
        text.append(emoji).append(" *동아리 검증 결과*\n\n");
        text.append("• *동아리명:* ").append(application.getClubName()).append("\n");
        text.append("• *분류:* ").append(application.getCategory().getDescription()).append("\n");
        text.append("• *위험 점수:* ").append(result.getTotalRiskScore()).append("점\n");
        text.append("• *판정 결과:* ").append(status).append("\n");

        if (application.getShortDescription() != null) {
            text.append("• *소개:* ").append(application.getShortDescription()).append("\n");
        }

        if (result.getLlmOverallObservation() != null) {
            String observation = result.getLlmOverallObservation();
            if (observation.length() > 300) {
                observation = observation.substring(0, 300) + "...";
            }
            text.append("\n*LLM 분석:*\n").append(observation).append("\n");
        }

        if (evidenceCards != null && !evidenceCards.isEmpty()) {
            text.append("\n*참고 링크:*\n");
            int count = 0;
            for (EvidenceCard card : evidenceCards) {
                if (card.getUrl() != null && !card.getUrl().isBlank() && count < 5) {
                    String title = card.getTitle() != null ? card.getTitle() : card.getUrl();
                    if (title.length() > 50) {
                        title = title.substring(0, 50) + "...";
                    }
                    text.append("• <").append(card.getUrl()).append("|").append(title).append(">\n");
                    count++;
                }
            }
        }

        text.append("\n_신청 ID: ").append(application.getId()).append("_");

        return SlackMessage.builder()
                .text(text.toString())
                .build();
    }

    private SlackMessage buildErrorMessage(ClubApplication application, String errorMessage) {
        StringBuilder text = new StringBuilder();
        text.append("⚠️ *동아리 검증 오류*\n\n");
        text.append("• *동아리명:* ").append(application.getClubName()).append("\n");
        text.append("• *신청 ID:* ").append(application.getId()).append("\n");
        text.append("• *오류:* ").append(errorMessage);

        return SlackMessage.builder()
                .text(text.toString())
                .build();
    }

    private String getEmojiForRiskLevel(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case AUTO_APPROVE -> "✅";
            case PENDING_REVIEW -> "⏸️";
            case MANUAL_REVIEW -> "🚨";
        };
    }

    private void sendMessage(SlackMessage message) {
        webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> log.debug("Slack notification sent successfully"))
                .doOnError(error -> log.error("Failed to send Slack notification: {}", error.getMessage()))
                .subscribe();
    }
}
