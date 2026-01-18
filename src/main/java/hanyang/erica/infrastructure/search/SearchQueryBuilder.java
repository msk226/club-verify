package hanyang.erica.infrastructure.search;

import hanyang.erica.domain.club.entity.ClubApplication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchQueryBuilder {

    private static final String SITE_HANYANG = "site:hanyang.ac.kr";
    private static final String SITE_INSTAGRAM = "site:instagram.com";
    private static final String SITE_EVERYTIME = "site:everytime.kr";

    public List<String> buildSearchQueries(ClubApplication application) {
        List<String> queries = new ArrayList<>();
        String clubName = application.getClubName();

        queries.add(buildOfficialUniversityQuery(clubName));

        queries.add(buildGeneralQuery(clubName));

        queries.add(buildSnsQuery(clubName));

        queries.add(buildCommunityQuery(clubName));

        if (application.getSubCategory() != null && !application.getSubCategory().isBlank()) {
            queries.add(buildCategoryQuery(clubName, application.getSubCategory()));
        }

        return queries;
    }

    public String buildOfficialUniversityQuery(String clubName) {
        return String.format("\"%s\" %s (동아리 OR 학생단체)", clubName, SITE_HANYANG);
    }

    public String buildGeneralQuery(String clubName) {
        return String.format("\"%s\" 한양대 동아리", clubName);
    }

    public String buildSnsQuery(String clubName) {
        return String.format("\"%s\" 한양대 %s", clubName, SITE_INSTAGRAM);
    }

    public String buildCommunityQuery(String clubName) {
        return String.format("\"%s\" 한양대 %s", clubName, SITE_EVERYTIME);
    }

    public String buildCategoryQuery(String clubName, String category) {
        return String.format("\"%s\" %s 동아리", clubName, category);
    }

    public String buildNewsQuery(String clubName) {
        return String.format("\"%s\" 한양대 뉴스 OR 기사", clubName);
    }

    public String buildRiskKeywordQuery(String clubName, String riskKeyword) {
        return String.format("\"%s\" \"%s\"", clubName, riskKeyword);
    }
}
