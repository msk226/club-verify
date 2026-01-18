package hanyang.erica.domain.verification.service;

import hanyang.erica.common.constant.RiskKeywords;
import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.evidence.entity.SourceType;
import hanyang.erica.domain.verification.entity.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RiskScoreCalculator {

    private static final int SCORE_OFFICIAL_SOURCE = -40;
    private static final int SCORE_SNS_LONG_ACTIVITY = -25;
    private static final int SCORE_THIRD_PARTY_MENTION = -20;
    private static final int SCORE_NO_SEARCH_RESULTS = 25;
    private static final int SCORE_HIGH_RISK_KEYWORDS = 30;
    private static final int SCORE_MEDIUM_RISK_KEYWORDS = 15;
    private static final int SCORE_DUPLICATE_CONTENT = 15;
    private static final int SCORE_NON_OFFICIAL_EMAIL = 10;

    private static final int LONG_ACTIVITY_MONTHS_THRESHOLD = 6;

    public RiskScoreResult calculate(ClubApplication application, List<EvidenceCard> evidenceCards) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int totalScore = 0;

        int officialSourceScore = calculateOfficialSourceScore(evidenceCards);
        if (officialSourceScore != 0) {
            breakdown.put("학교공식/학과공지 출처", officialSourceScore);
            totalScore += officialSourceScore;
        }

        int snsActivityScore = calculateSnsActivityScore(evidenceCards);
        if (snsActivityScore != 0) {
            breakdown.put("SNS 장기 활동", snsActivityScore);
            totalScore += snsActivityScore;
        }

        int thirdPartyScore = calculateThirdPartyMentionScore(evidenceCards);
        if (thirdPartyScore != 0) {
            breakdown.put("제3자 언급", thirdPartyScore);
            totalScore += thirdPartyScore;
        }

        int noResultScore = calculateNoSearchResultScore(evidenceCards, application);
        if (noResultScore != 0) {
            breakdown.put("검색결과 부족/최근생성", noResultScore);
            totalScore += noResultScore;
        }

        int riskKeywordScore = calculateRiskKeywordScore(application, evidenceCards);
        if (riskKeywordScore != 0) {
            breakdown.put("위험 키워드", riskKeywordScore);
            totalScore += riskKeywordScore;
        }

        int duplicateScore = calculateDuplicateContentScore(evidenceCards);
        if (duplicateScore != 0) {
            breakdown.put("복제 콘텐츠", duplicateScore);
            totalScore += duplicateScore;
        }

        int emailDomainScore = calculateEmailDomainScore(application);
        if (emailDomainScore != 0) {
            breakdown.put("비공식 이메일 도메인", emailDomainScore);
            totalScore += emailDomainScore;
        }

        RiskLevel riskLevel = RiskLevel.fromScore(totalScore);

        log.info("Risk score calculated for club {}: totalScore={}, riskLevel={}, breakdown={}",
                application.getClubName(), totalScore, riskLevel, breakdown);

        return new RiskScoreResult(totalScore, riskLevel, breakdown);
    }

    private int calculateOfficialSourceScore(List<EvidenceCard> evidenceCards) {
        boolean hasOfficialSource = evidenceCards.stream()
                .anyMatch(card -> card.getSourceType() == SourceType.OFFICIAL_SCHOOL ||
                        card.getSourceType() == SourceType.DEPARTMENT_NOTICE ||
                        (card.getSignals() != null && Boolean.TRUE.equals(card.getSignals().getIsOfficialSource())));

        return hasOfficialSource ? SCORE_OFFICIAL_SOURCE : 0;
    }

    private int calculateSnsActivityScore(List<EvidenceCard> evidenceCards) {
        boolean hasLongTermSns = evidenceCards.stream()
                .filter(card -> card.getSourceType() == SourceType.SNS)
                .anyMatch(card -> card.getSignals() != null &&
                        card.getSignals().getActivityDurationMonths() != null &&
                        card.getSignals().getActivityDurationMonths() >= LONG_ACTIVITY_MONTHS_THRESHOLD);

        return hasLongTermSns ? SCORE_SNS_LONG_ACTIVITY : 0;
    }

    private int calculateThirdPartyMentionScore(List<EvidenceCard> evidenceCards) {
        long thirdPartyCount = evidenceCards.stream()
                .filter(card -> card.getSourceType() == SourceType.NEWS ||
                        card.getSourceType() == SourceType.COMMUNITY ||
                        card.getSourceType() == SourceType.BLOG)
                .filter(card -> card.getSignals() == null ||
                        !Boolean.TRUE.equals(card.getSignals().getIsOfficialSource()))
                .count();

        return thirdPartyCount >= 1 ? SCORE_THIRD_PARTY_MENTION : 0;
    }

    private int calculateNoSearchResultScore(List<EvidenceCard> evidenceCards, ClubApplication application) {
        if (evidenceCards.size() <= 1) {
            boolean isRecentlyCreated = application.getCreatedAt() != null &&
                    application.getCreatedAt().toLocalDate().isAfter(LocalDate.now().minusMonths(3));

            if (isRecentlyCreated) {
                return SCORE_NO_SEARCH_RESULTS;
            }
        }
        return 0;
    }

    private int calculateRiskKeywordScore(ClubApplication application, List<EvidenceCard> evidenceCards) {
        int score = 0;

        String combinedText = buildCombinedText(application, evidenceCards);

        long highRiskCount = RiskKeywords.countHighRiskKeywords(combinedText);
        if (highRiskCount > 0) {
            score += SCORE_HIGH_RISK_KEYWORDS;
            if (highRiskCount >= 3) {
                score += 10;
            }
        }

        if (RiskKeywords.containsMediumRiskKeyword(combinedText) && !RiskKeywords.containsHighRiskKeyword(combinedText)) {
            score += SCORE_MEDIUM_RISK_KEYWORDS;
        }

        return score;
    }

    private String buildCombinedText(ClubApplication application, List<EvidenceCard> evidenceCards) {
        StringBuilder sb = new StringBuilder();
        sb.append(nullSafe(application.getClubName())).append(" ");
        sb.append(nullSafe(application.getShortDescription())).append(" ");
        sb.append(nullSafe(application.getFullDescription())).append(" ");

        for (EvidenceCard card : evidenceCards) {
            sb.append(nullSafe(card.getTitle())).append(" ");
            sb.append(nullSafe(card.getSnippet())).append(" ");
        }

        return sb.toString();
    }

    private int calculateDuplicateContentScore(List<EvidenceCard> evidenceCards) {
        if (evidenceCards.size() < 3) {
            return 0;
        }

        Map<String, Integer> snippetCount = new java.util.HashMap<>();
        for (EvidenceCard card : evidenceCards) {
            if (card.getSnippet() != null && card.getSnippet().length() > 50) {
                String normalized = card.getSnippet().substring(0, 50).toLowerCase().trim();
                snippetCount.merge(normalized, 1, Integer::sum);
            }
        }

        boolean hasDuplicates = snippetCount.values().stream().anyMatch(count -> count >= 3);
        return hasDuplicates ? SCORE_DUPLICATE_CONTENT : 0;
    }

    private int calculateEmailDomainScore(ClubApplication application) {
        String domain = application.getRepresentativeEmailDomain();
        if (domain == null || domain.isBlank()) {
            return SCORE_NON_OFFICIAL_EMAIL;
        }

        return RiskKeywords.isOfficialUniversityDomain(domain) ? 0 : SCORE_NON_OFFICIAL_EMAIL;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    public record RiskScoreResult(
            int totalScore,
            RiskLevel riskLevel,
            Map<String, Integer> breakdown
    ) {
    }
}
