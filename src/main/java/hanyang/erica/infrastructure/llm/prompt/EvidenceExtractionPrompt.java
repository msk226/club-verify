package hanyang.erica.infrastructure.llm.prompt;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.infrastructure.search.dto.SearchResponse;

import java.util.List;

public class EvidenceExtractionPrompt {

    private static final String SYSTEM_PROMPT = """
            당신은 대학 동아리 검증 시스템의 증거 분석 전문가입니다.
            검색 결과를 분석하여 동아리의 실체와 정당성을 판단할 수 있는 증거를 추출합니다.
            

            ## 분류 기준

            ### 출처 유형 (sourceType)
            - OFFICIAL_SCHOOL: 학교 공식 웹사이트, 학과 공지사항
            - DEPARTMENT_NOTICE: 학과/단과대 공지
            - SNS: Instagram, Facebook, Twitter 등
            - NEWS: 뉴스 기사
            - BLOG: 네이버 블로그, 티스토리 등
            - COMMUNITY: 에브리타임, 대학 커뮤니티
            - UNKNOWN: 분류 불가

            ### 시그널 강도 (signalStrength: -10 ~ +10)
            - 음수: 정상 동아리임을 뒷받침하는 증거
            - 양수: 의심스러운 요소를 나타내는 증거

            ### 위험 키워드
            고위험: 전도, 선교, 기도, 포교, 복음, 구원, 신천지, JMS, 다단계, MLM, 피라미드
            중위험: 성경공부, 예배, 투자, 수익, 코인, 부업

            ## 응답 형식
            반드시 아래 JSON 형식으로만 응답하세요:

            ```json
            {
              "evidences": [
                {
                  "evidenceId": "E1",
                  "sourceType": "OFFICIAL_SCHOOL",
                  "url": "URL",
                  "title": "제목",
                  "snippet": "요약 (200-500자)",
                  "signalStrength": -5,
                  "signalType": "공식인증",
                  "detectedRiskKeywords": [],
                  "isOfficialSource": true,
                  "estimatedActivityMonths": 24
                }
              ],
              "summary": "전체 검색 결과 요약 (1-2문장)"
            }
            ```
            """;

    public static String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static String buildUserPrompt(ClubApplication application, List<SearchResponse> searchResults) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## 검증 대상 동아리 정보\n");
        prompt.append("- 동아리명: ").append(application.getClubName()).append("\n");
        prompt.append("- 분류: ").append(application.getCategory().getDescription());
        if (application.getSubCategory() != null) {
            prompt.append(" / ").append(application.getSubCategory());
        }
        prompt.append("\n");
        prompt.append("- 한 줄 소개: ").append(nullSafe(application.getShortDescription())).append("\n");
        prompt.append("- 상세 설명: ").append(nullSafe(application.getFullDescription())).append("\n\n");

        prompt.append("## 검색 결과\n\n");

        int totalResults = 0;
        for (SearchResponse response : searchResults) {
            if (response.getItems() != null && !response.getItems().isEmpty()) {
                prompt.append("### 검색어: ").append(response.getQuery()).append("\n");
                prompt.append("총 결과 수: ").append(response.getTotalResults()).append("\n\n");

                for (var item : response.getItems()) {
                    totalResults++;
                    prompt.append("#### 결과 ").append(totalResults).append("\n");
                    prompt.append("- 제목: ").append(item.getTitle()).append("\n");
                    prompt.append("- URL: ").append(item.getLink()).append("\n");
                    prompt.append("- 도메인: ").append(item.getDisplayLink()).append("\n");
                    prompt.append("- 스니펫: ").append(item.getSnippet()).append("\n\n");
                }
            }
        }

        if (totalResults == 0) {
            prompt.append("검색 결과가 없습니다.\n");
        }

        prompt.append("\n위 검색 결과를 분석하여 증거 카드를 추출해주세요.");

        return prompt.toString();
    }

    private static String nullSafe(String value) {
        return value != null ? value : "(없음)";
    }
}
