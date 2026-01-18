package hanyang.erica.domain.club.entity;

public enum ClubCategory {
    CENTRAL("중앙동아리"),
    UNION("연합동아리"),
    COLLEGE("단과대학동아리"),
    DEPARTMENT("학과동아리");

    private final String description;

    ClubCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
