package hanyang.erica.common.constant;

import java.util.List;
import java.util.Set;

public final class RiskKeywords {

    private RiskKeywords() {
    }

    public static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "전도",
            "선교",
            "기도",
            "포교",
            "복음",
            "구원",
            "신천지",
            "JMS",
            "만민중앙교회",
            "통일교",
            "세계평화통일가정연합",
            "다단계",
            "MLM",
            "네트워크마케팅",
            "피라미드",
            "암웨이",
            "애터미"
    );

    public static final Set<String> MEDIUM_RISK_KEYWORDS = Set.of(
            "성경공부",
            "말씀",
            "예배",
            "찬양",
            "신앙",
            "영성",
            "투자",
            "수익",
            "재테크",
            "코인",
            "주식",
            "부업"
    );

    public static final Set<String> SUSPICIOUS_PATTERNS = Set.of(
            "무료",
            "대학생 환영",
            "누구나 참여",
            "인생이 바뀌",
            "성공",
            "자기계발",
            "리더십"
    );

    public static final List<String> OFFICIAL_UNIVERSITY_DOMAINS = List.of(
            "hanyang.ac.kr",
            "hyu.ac.kr"
    );

    public static boolean containsHighRiskKeyword(String text) {
        if (text == null) return false;
        String lowerText = text.toLowerCase();
        return HIGH_RISK_KEYWORDS.stream().anyMatch(keyword -> lowerText.contains(keyword.toLowerCase()));
    }

    public static boolean containsMediumRiskKeyword(String text) {
        if (text == null) return false;
        String lowerText = text.toLowerCase();
        return MEDIUM_RISK_KEYWORDS.stream().anyMatch(keyword -> lowerText.contains(keyword.toLowerCase()));
    }

    public static long countHighRiskKeywords(String text) {
        if (text == null) return 0;
        String lowerText = text.toLowerCase();
        return HIGH_RISK_KEYWORDS.stream().filter(keyword -> lowerText.contains(keyword.toLowerCase())).count();
    }

    public static boolean isOfficialUniversityDomain(String domain) {
        if (domain == null) return false;
        return OFFICIAL_UNIVERSITY_DOMAINS.stream().anyMatch(domain::endsWith);
    }
}
