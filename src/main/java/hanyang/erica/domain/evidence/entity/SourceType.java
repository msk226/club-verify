package hanyang.erica.domain.evidence.entity;

public enum SourceType {
    OFFICIAL_SCHOOL("학교공식"),
    DEPARTMENT_NOTICE("학과공지"),
    SNS("SNS"),
    NEWS("뉴스"),
    BLOG("블로그"),
    COMMUNITY("커뮤니티"),
    UNKNOWN("미분류");

    private final String description;

    SourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
