package hanyang.erica.infrastructure.llm.prompt;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.verification.entity.RiskLevel;

import java.util.List;
import java.util.Map;

public class DeepAnalysisPrompt {

    private static final String SYSTEM_PROMPT = """
            당신은 대학 동아리 검증 시스템의 심층 분석 전문가입니다.
            수집된 증거와 규칙 기반 점수를 바탕으로 종합적인 분석 의견을 제시합니다.

            ## 분석 관점

            1. **정당성 검증**: 실제 존재하고 정상적으로 활동하는 동아리인가?
            2. **위험 요소 식별**: 사이비/종교 전도, 다단계, 사기 등의 징후가 있는가?
            3. **활동 지속성**: 지속적인 활동 이력이 확인되는가?
            4. **공식 인증**: 학교/학과 차원의 공식 인증이 있는가?

            ## 이상 신호 유형
            - NO_ONLINE_PRESENCE: 온라인 존재감 부재
            - RELIGIOUS_INDICATORS: 종교 관련 징후
            - MLM_INDICATORS: 다단계/피라미드 징후
            - RECENTLY_CREATED: 최근 급조 의심
            - COPIED_CONTENT: 복제/어뷰징 패턴
            - SUSPICIOUS_DOMAIN: 의심스러운 외부 도메인
            - INCONSISTENT_INFO: 정보 불일치

            ## 응답 형식
            반드시 아래 JSON 형식으로만 응답하세요:

            ```json
            {
              "overallObservation": "종합 분석 의견 (3-5문장)",
              "anomalies": [
                {
                  "signalType": "RELIGIOUS_INDICATORS",
                  "description": "구체적인 이상 신호 설명",
                  "severity": 7,
                  "evidence": "근거가 되는 증거 ID 또는 내용"
                }
              ],
              "recommendation": "승인/보류/반려 중 하나와 그 이유",
              "confidenceScore": 85
            }
            ```

            confidenceScore: 분석 결과에 대한 확신도 (0-100)
            severity: 이상 신호의 심각도 (1-10)
            """;

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String buildUserPrompt(
            ClubApplication application,
            List<EvidenceCard> evidenceCards,
            int riskScore,
            RiskLevel riskLevel,
            Map<String, Integer> scoreBreakdown) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("## 검증 대상 동아리 정보\n");
        prompt.append("- 동아리명: ").append(application.getClubName()).append("\n");
        prompt.append("- 분류: ").append(application.getCategory().getDescription());
        if (application.getSubCategory() != null) {
            prompt.append(" / ").append(application.getSubCategory());
        }
        prompt.append("\n");
        prompt.append("- 한 줄 소개: ").append(nullSafe(application.getShortDescription())).append("\n");
        prompt.append("- 상세 설명: ").append(nullSafe(application.getFullDescription())).append("\n");
        prompt.append("- 대표 이메일 도메인: ").append(nullSafe(application.getRepresentativeEmailDomain())).append("\n\n");

        prompt.append("## 규칙 기반 점수\n");
        prompt.append("- 총 위험 점수: ").append(riskScore).append("\n");
        prompt.append("- 위험 등급: ").append(riskLevel.getDescription()).append("\n");
        prompt.append("- 점수 상세:\n");
        for (var entry : scoreBreakdown.entrySet()) {
            prompt.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        prompt.append("\n");

        prompt.append("## 수집된 증거 카드\n\n");

        if (evidenceCards.isEmpty()) {
            prompt.append("수집된 증거가 없습니다.\n");
        } else {
            for (EvidenceCard card : evidenceCards) {
                prompt.append("### ").append(card.getEvidenceId()).append("\n");
                prompt.append("- 출처 유형: ").append(card.getSourceType().getDescription()).append("\n");
                prompt.append("- URL: ").append(nullSafe(card.getUrl())).append("\n");
                prompt.append("- 제목: ").append(nullSafe(card.getTitle())).append("\n");
                prompt.append("- 요약: ").append(nullSafe(card.getSnippet())).append("\n");
                if (card.getSignals() != null) {
                    prompt.append("- 시그널 강도: ").append(card.getSignals().getStrength()).append("\n");
                    prompt.append("- 시그널 유형: ").append(card.getSignals().getType()).append("\n");
                    if (card.getSignals().getRiskKeywords() != null) {
                        prompt.append("- 탐지된 위험 키워드: ").append(card.getSignals().getRiskKeywords()).append("\n");
                    }
                }
                prompt.append("\n");
            }
        }

        prompt.append("\n위 정보를 바탕으로 심층 분석을 수행해주세요.");

        return prompt.toString();
    }

    private static String nullSafe(String value) {
        return value != null ? value : "(없음)";
    }
}
