package hanyang.erica.domain.club.entity;

public enum VerificationStatus {
    PENDING("접수대기"),
    VERIFICATION_IN_PROGRESS("검증진행중"),
    AUTO_APPROVED("자동승인"),
    PENDING_REVIEW("보류-추가검토필요"),
    MANUAL_REVIEW_REQUIRED("반려-운영자검토필요"),
    APPROVED("승인완료"),
    REJECTED("반려");

    private final String description;

    VerificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
