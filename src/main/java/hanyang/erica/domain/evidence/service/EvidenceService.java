package hanyang.erica.domain.evidence.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.evidence.entity.EvidenceCard;
import hanyang.erica.domain.evidence.entity.EvidenceSignal;
import hanyang.erica.domain.evidence.repository.EvidenceCardRepository;
import hanyang.erica.infrastructure.llm.LlmClient;
import hanyang.erica.infrastructure.llm.dto.EvidenceExtractionResult;
import hanyang.erica.infrastructure.llm.dto.LlmRequest;
import hanyang.erica.infrastructure.llm.dto.LlmResponse;
import hanyang.erica.infrastructure.llm.prompt.EvidenceExtractionPrompt;
import hanyang.erica.infrastructure.search.SearchClient;
import hanyang.erica.infrastructure.search.SearchQueryBuilder;
import hanyang.erica.infrastructure.search.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final SearchClient searchClient;
    private final SearchQueryBuilder searchQueryBuilder;
    private final LlmClient llmClient;
    private final EvidenceCardRepository evidenceCardRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");

    @Transactional
    public Mono<List<EvidenceCard>> collectAndAnalyzeEvidence(ClubApplication application) {
        log.info("Starting evidence collection for club: {}", application.getClubName());

        List<String> queries = searchQueryBuilder.buildSearchQueries(application);

        return Flux.fromIterable(queries)
                .flatMap(query -> searchClient.search(query, 5))
                .collectList()
                .flatMap(searchResults -> analyzeWithLlm(application, searchResults))
                .map(extractionResult -> createEvidenceCards(application, extractionResult))
                .flatMap(cards -> {
                    List<EvidenceCard> saved = evidenceCardRepository.saveAll(cards);
                    return Mono.just(saved);
                })
                .doOnSuccess(cards -> log.info("Evidence collection completed: {} cards for club: {}",
                        cards.size(), application.getClubName()))
                .doOnError(error -> log.error("Evidence collection failed for club: {}. Error: {}",
                        application.getClubName(), error.getMessage()));
    }

    public Mono<List<SearchResponse>> executeSearchQueries(ClubApplication application) {
        List<String> queries = searchQueryBuilder.buildSearchQueries(application);

        return Flux.fromIterable(queries)
                .flatMap(query -> searchClient.search(query, 5))
                .collectList();
    }

    private Mono<EvidenceExtractionResult> analyzeWithLlm(
            ClubApplication application,
            List<SearchResponse> searchResults) {

        String systemPrompt = EvidenceExtractionPrompt.getSystemPrompt();
        String userPrompt = EvidenceExtractionPrompt.buildUserPrompt(application, searchResults);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .maxTokens(4096)
                .temperature(0.3)
                .build();

        return llmClient.chat(request)
                .map(this::parseExtractionResult)
                .onErrorResume(e -> {
                    log.error("LLM analysis failed, creating minimal evidence from search results", e);
                    return Mono.just(createMinimalExtractionResult(searchResults));
                });
    }

    private EvidenceExtractionResult parseExtractionResult(LlmResponse response) {
        if (!response.isSuccess()) {
            log.warn("LLM response was not successful: {}", response.getErrorMessage());
            return EvidenceExtractionResult.builder()
                    .evidences(new ArrayList<>())
                    .summary("LLM 분석 실패")
                    .build();
        }

        String content = response.getContent();
        String jsonContent = extractJson(content);

        try {
            return objectMapper.readValue(jsonContent, EvidenceExtractionResult.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response as JSON: {}", e.getMessage());
            return EvidenceExtractionResult.builder()
                    .evidences(new ArrayList<>())
                    .summary("응답 파싱 실패: " + content.substring(0, Math.min(200, content.length())))
                    .build();
        }
    }

    private String extractJson(String content) {
        Matcher matcher = JSON_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content.trim();
    }

    private EvidenceExtractionResult createMinimalExtractionResult(List<SearchResponse> searchResults) {
        List<EvidenceExtractionResult.ExtractedEvidence> evidences = new ArrayList<>();
        int idx = 1;

        for (SearchResponse response : searchResults) {
            if (response.getItems() != null) {
                for (var item : response.getItems()) {
                    evidences.add(EvidenceExtractionResult.ExtractedEvidence.builder()
                            .evidenceId("E" + idx++)
                            .sourceType(inferSourceType(item.getDisplayLink()))
                            .url(item.getLink())
                            .title(item.getTitle())
                            .snippet(item.getSnippet())
                            .signalStrength(0)
                            .signalType("AUTO_GENERATED")
                            .detectedRiskKeywords(new ArrayList<>())
                            .isOfficialSource(isOfficialDomain(item.getDisplayLink()))
                            .build());
                }
            }
        }

        return EvidenceExtractionResult.builder()
                .evidences(evidences)
                .summary("LLM 분석 없이 검색 결과에서 자동 생성됨")
                .build();
    }

    private hanyang.erica.domain.evidence.entity.SourceType inferSourceType(String domain) {
        if (domain == null) return hanyang.erica.domain.evidence.entity.SourceType.UNKNOWN;

        if (domain.contains("hanyang.ac.kr") || domain.contains("hyu.ac.kr")) {
            return hanyang.erica.domain.evidence.entity.SourceType.OFFICIAL_SCHOOL;
        } else if (domain.contains("instagram.com") || domain.contains("facebook.com") || domain.contains("twitter.com")) {
            return hanyang.erica.domain.evidence.entity.SourceType.SNS;
        } else if (domain.contains("everytime.kr")) {
            return hanyang.erica.domain.evidence.entity.SourceType.COMMUNITY;
        } else if (domain.contains("blog") || domain.contains("tistory")) {
            return hanyang.erica.domain.evidence.entity.SourceType.BLOG;
        } else if (domain.contains("news") || domain.contains("press")) {
            return hanyang.erica.domain.evidence.entity.SourceType.NEWS;
        }
        return hanyang.erica.domain.evidence.entity.SourceType.UNKNOWN;
    }

    private boolean isOfficialDomain(String domain) {
        if (domain == null) return false;
        return domain.contains("hanyang.ac.kr") || domain.contains("hyu.ac.kr");
    }

    private List<EvidenceCard> createEvidenceCards(
            ClubApplication application,
            EvidenceExtractionResult extractionResult) {

        List<EvidenceCard> cards = new ArrayList<>();

        if (extractionResult.getEvidences() == null) {
            return cards;
        }

        for (var evidence : extractionResult.getEvidences()) {
            EvidenceSignal signal = EvidenceSignal.builder()
                    .strength(evidence.getSignalStrength())
                    .type(evidence.getSignalType())
                    .riskKeywords(evidence.getDetectedRiskKeywords() != null ?
                            String.join(",", evidence.getDetectedRiskKeywords()) : null)
                    .isOfficialSource(evidence.getIsOfficialSource())
                    .activityDurationMonths(evidence.getEstimatedActivityMonths())
                    .build();

            EvidenceCard card = EvidenceCard.builder()
                    .evidenceId(evidence.getEvidenceId())
                    .clubApplication(application)
                    .sourceType(evidence.getSourceType())
                    .url(evidence.getUrl())
                    .title(evidence.getTitle())
                    .snippet(evidence.getSnippet())
                    .signals(signal)
                    .build();

            cards.add(card);
        }

        return cards;
    }

    public List<EvidenceCard> findByClubApplication(ClubApplication application) {
        return evidenceCardRepository.findByClubApplication(application);
    }
}
