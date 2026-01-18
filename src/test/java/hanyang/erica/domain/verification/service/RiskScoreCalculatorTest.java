package hanyang.erica.domain.verification.service;

import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.ClubCategory;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.evidence.entity.EvidenceSignal;
import hanyang.erica.domain.evidence.entity.SourceType;
import hanyang.erica.domain.verification.entity.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoreCalculatorTest {

    private RiskScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculator();
    }

    @Nested
    @DisplayName("공식 출처 점수 계산")
    class OfficialSourceScoreTest {

        @Test
        @DisplayName("학교 공식 출처가 있으면 -40점")
        void shouldDeduct40PointsForOfficialSource() {
            ClubApplication application = createBasicApplication();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.OFFICIAL_SCHOOL, null)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.breakdown()).containsEntry("학교공식/학과공지 출처", -40);
        }

        @Test
        @DisplayName("학과 공지 출처가 있으면 -40점")
        void shouldDeduct40PointsForDepartmentNotice() {
            ClubApplication application = createBasicApplication();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.DEPARTMENT_NOTICE, null)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.breakdown()).containsEntry("학교공식/학과공지 출처", -40);
        }
    }

    @Nested
    @DisplayName("SNS 활동 점수 계산")
    class SnsActivityScoreTest {

        @Test
        @DisplayName("6개월 이상 SNS 활동이 있으면 -25점")
        void shouldDeduct25PointsForLongTermSnsActivity() {
            ClubApplication application = createBasicApplication();
            EvidenceSignal signal = EvidenceSignal.builder()
                    .activityDurationMonths(12)
                    .build();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.SNS, signal)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.breakdown()).containsEntry("SNS 장기 활동", -25);
        }

        @Test
        @DisplayName("6개월 미만 SNS 활동은 점수 변동 없음")
        void shouldNotDeductPointsForShortTermSnsActivity() {
            ClubApplication application = createBasicApplication();
            EvidenceSignal signal = EvidenceSignal.builder()
                    .activityDurationMonths(3)
                    .build();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.SNS, signal)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.breakdown()).doesNotContainKey("SNS 장기 활동");
        }
    }

    @Nested
    @DisplayName("제3자 언급 점수 계산")
    class ThirdPartyMentionScoreTest {

        @Test
        @DisplayName("뉴스 언급이 있으면 -20점")
        void shouldDeduct20PointsForNewsMention() {
            ClubApplication application = createBasicApplication();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.NEWS, null)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.breakdown()).containsEntry("제3자 언급", -20);
        }

        @Test
        @DisplayName("커뮤니티 언급이 있으면 -20점")
        void shouldDeduct20PointsForCommunityMention() {
            ClubApplication application = createBasicApplication();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.COMMUNITY, null)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.breakdown()).containsEntry("제3자 언급", -20);
        }
    }

    @Nested
    @DisplayName("위험 키워드 점수 계산")
    class RiskKeywordScoreTest {

        @Test
        @DisplayName("고위험 키워드(전도)가 포함되면 +30점")
        void shouldAdd30PointsForHighRiskKeyword() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .shortDescription("전도와 선교를 목적으로 하는 동아리")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            assertThat(result.breakdown()).containsEntry("위험 키워드", 30);
        }

        @Test
        @DisplayName("고위험 키워드가 3개 이상이면 +40점")
        void shouldAdd40PointsForMultipleHighRiskKeywords() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .shortDescription("전도, 선교, 기도, 포교를 목적으로 하는 동아리")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            assertThat(result.breakdown()).containsEntry("위험 키워드", 40);
        }

        @Test
        @DisplayName("중위험 키워드만 있으면 +15점")
        void shouldAdd15PointsForMediumRiskKeywordOnly() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .shortDescription("성경공부와 예배를 함께하는 동아리")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            assertThat(result.breakdown()).containsEntry("위험 키워드", 15);
        }
    }

    @Nested
    @DisplayName("이메일 도메인 점수 계산")
    class EmailDomainScoreTest {

        @Test
        @DisplayName("공식 대학 이메일 도메인이면 점수 변동 없음")
        void shouldNotAddPointsForOfficialDomain() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .representativeEmailDomain("hanyang.ac.kr")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            assertThat(result.breakdown()).doesNotContainKey("비공식 이메일 도메인");
        }

        @Test
        @DisplayName("비공식 이메일 도메인이면 +10점")
        void shouldAdd10PointsForNonOfficialDomain() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .representativeEmailDomain("gmail.com")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            assertThat(result.breakdown()).containsEntry("비공식 이메일 도메인", 10);
        }
    }

    @Nested
    @DisplayName("위험 등급 판정")
    class RiskLevelDeterminationTest {

        @Test
        @DisplayName("총점 20점 이하면 AUTO_APPROVE")
        void shouldReturnAutoApproveForLowScore() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .representativeEmailDomain("hanyang.ac.kr")
                    .build();
            List<EvidenceCard> evidenceCards = List.of(
                    createEvidenceCard(SourceType.OFFICIAL_SCHOOL, null),
                    createEvidenceCard(SourceType.NEWS, null)
            );

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, evidenceCards);

            assertThat(result.riskLevel()).isEqualTo(RiskLevel.AUTO_APPROVE);
        }

        @Test
        @DisplayName("총점 21~60점이면 PENDING_REVIEW")
        void shouldReturnPendingReviewForMediumScore() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .shortDescription("성경공부와 예배를 함께하는 동아리")
                    .representativeEmailDomain("gmail.com")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            assertThat(result.riskLevel()).isEqualTo(RiskLevel.PENDING_REVIEW);
        }

        @Test
        @DisplayName("고위험 키워드가 많으면 높은 위험 점수")
        void shouldReturnHighScoreForMultipleHighRiskKeywords() {
            ClubApplication application = ClubApplication.builder()
                    .clubName("테스트동아리")
                    .category(ClubCategory.CENTRAL)
                    .shortDescription("전도와 선교, 기도, 포교를 목적으로 하는 동아리")
                    .fullDescription("신천지 관련 다단계 MLM 네트워크마케팅 활동")
                    .representativeEmailDomain("gmail.com")
                    .build();

            RiskScoreCalculator.RiskScoreResult result = calculator.calculate(application, new ArrayList<>());

            // 40점(위험키워드) + 10점(비공식이메일) = 50점 -> PENDING_REVIEW
            assertThat(result.totalScore()).isGreaterThanOrEqualTo(40);
            assertThat(result.riskLevel()).isIn(RiskLevel.PENDING_REVIEW, RiskLevel.MANUAL_REVIEW);
        }
    }

    private ClubApplication createBasicApplication() {
        return ClubApplication.builder()
                .clubName("테스트동아리")
                .category(ClubCategory.CENTRAL)
                .shortDescription("테스트 동아리입니다")
                .representativeEmailDomain("hanyang.ac.kr")
                .build();
    }

    private EvidenceCard createEvidenceCard(SourceType sourceType, EvidenceSignal signal) {
        return EvidenceCard.builder()
                .evidenceId("E1")
                .sourceType(sourceType)
                .url("https://example.com")
                .title("테스트")
                .snippet("테스트 스니펫")
                .signals(signal)
                .build();
    }
}
