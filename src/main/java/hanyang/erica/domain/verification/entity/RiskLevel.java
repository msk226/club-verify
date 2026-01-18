package hanyang.erica.domain.verification.entity;

public enum RiskLevel {
    AUTO_APPROVE("자동승인후보", 20),
    PENDING_REVIEW("보류-추가서류", 60),
    MANUAL_REVIEW("반려-운영자검토", Integer.MAX_VALUE);

    private final String description;
    private final int maxScore;

    RiskLevel(String description, int maxScore) {
        this.description = description;
        this.maxScore = maxScore;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public static RiskLevel fromScore(int score) {
        if (score <= 20) {
            return AUTO_APPROVE;
        } else if (score <= 60) {
            return PENDING_REVIEW;
        } else {
            return MANUAL_REVIEW;
        }
    }
}
