package hanyang.erica.infrastructure.search;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.ClubCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchQueryBuilderTest {

    private SearchQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new SearchQueryBuilder();
    }

    @Test
    @DisplayName("동아리 신청 정보로 검색 쿼리 목록을 생성한다")
    void shouldBuildSearchQueriesFromApplication() {
        ClubApplication application = ClubApplication.builder()
                .clubName("한양밴드")
                .category(ClubCategory.CENTRAL)
                .subCategory("음악")
                .build();

        List<String> queries = queryBuilder.buildSearchQueries(application);

        assertThat(queries).hasSize(5);
        assertThat(queries).anyMatch(q -> q.contains("site:hanyang.ac.kr"));
        assertThat(queries).anyMatch(q -> q.contains("한양밴드"));
    }

    @Test
    @DisplayName("세부 카테고리가 없으면 4개의 쿼리만 생성한다")
    void shouldBuildFourQueriesWithoutSubCategory() {
        ClubApplication application = ClubApplication.builder()
                .clubName("한양밴드")
                .category(ClubCategory.CENTRAL)
                .build();

        List<String> queries = queryBuilder.buildSearchQueries(application);

        assertThat(queries).hasSize(4);
    }

    @Test
    @DisplayName("공식 대학교 쿼리는 site:hanyang.ac.kr을 포함한다")
    void shouldIncludeHanyangSiteInOfficialQuery() {
        String query = queryBuilder.buildOfficialUniversityQuery("테스트동아리");

        assertThat(query).contains("site:hanyang.ac.kr");
        assertThat(query).contains("\"테스트동아리\"");
        assertThat(query).contains("동아리 OR 학생단체");
    }

    @Test
    @DisplayName("일반 쿼리는 한양대 동아리를 포함한다")
    void shouldIncludeHanyangInGeneralQuery() {
        String query = queryBuilder.buildGeneralQuery("테스트동아리");

        assertThat(query).contains("\"테스트동아리\"");
        assertThat(query).contains("한양대 동아리");
    }

    @Test
    @DisplayName("SNS 쿼리는 인스타그램 사이트를 포함한다")
    void shouldIncludeInstagramSiteInSnsQuery() {
        String query = queryBuilder.buildSnsQuery("테스트동아리");

        assertThat(query).contains("site:instagram.com");
        assertThat(query).contains("\"테스트동아리\"");
    }

    @Test
    @DisplayName("커뮤니티 쿼리는 에브리타임 사이트를 포함한다")
    void shouldIncludeEverytimeSiteInCommunityQuery() {
        String query = queryBuilder.buildCommunityQuery("테스트동아리");

        assertThat(query).contains("site:everytime.kr");
        assertThat(query).contains("\"테스트동아리\"");
    }

    @Test
    @DisplayName("카테고리 쿼리는 카테고리와 동아리를 포함한다")
    void shouldIncludeCategoryInCategoryQuery() {
        String query = queryBuilder.buildCategoryQuery("테스트동아리", "음악");

        assertThat(query).contains("\"테스트동아리\"");
        assertThat(query).contains("음악");
        assertThat(query).contains("동아리");
    }

    @Test
    @DisplayName("뉴스 쿼리는 뉴스 OR 기사를 포함한다")
    void shouldIncludeNewsKeywordsInNewsQuery() {
        String query = queryBuilder.buildNewsQuery("테스트동아리");

        assertThat(query).contains("\"테스트동아리\"");
        assertThat(query).contains("뉴스 OR 기사");
    }

    @Test
    @DisplayName("위험 키워드 쿼리는 동아리명과 키워드를 포함한다")
    void shouldIncludeRiskKeywordInQuery() {
        String query = queryBuilder.buildRiskKeywordQuery("테스트동아리", "전도");

        assertThat(query).contains("\"테스트동아리\"");
        assertThat(query).contains("\"전도\"");
    }
}
