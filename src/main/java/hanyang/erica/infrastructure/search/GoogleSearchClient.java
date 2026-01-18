package hanyang.erica.infrastructure.search;

import hanyang.erica.infrastructure.search.dto.GoogleSearchApiResponse;
import hanyang.erica.infrastructure.search.dto.SearchResponse;
import hanyang.erica.infrastructure.search.dto.SearchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GoogleSearchClient implements SearchClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String cx;
    private final String baseUrl;
    private final int timeout;

    public GoogleSearchClient(
            WebClient webClient,
            @Value("${external.google.search.api-key:}") String apiKey,
            @Value("${external.google.search.cx:}") String cx,
            @Value("${external.google.search.base-url:https://www.googleapis.com/customsearch/v1}") String baseUrl,
            @Value("${external.google.search.timeout:10000}") int timeout) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.cx = cx;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    @Override
    @Retry(name = "searchApi", fallbackMethod = "searchFallback")
    @CircuitBreaker(name = "searchApi", fallbackMethod = "searchFallback")
    public Mono<SearchResponse> search(String query) {
        return search(query, 10);
    }

    @Override
    @Retry(name = "searchApi", fallbackMethod = "searchFallback")
    @CircuitBreaker(name = "searchApi", fallbackMethod = "searchFallback")
    public Mono<SearchResponse> search(String query, int maxResults) {
        if (apiKey == null || apiKey.isBlank() || cx == null || cx.isBlank()) {
            log.warn("Google Search API credentials not configured. Returning empty result.");
            return Mono.just(SearchResponse.builder()
                    .query(query)
                    .totalResults(0)
                    .items(Collections.emptyList())
                    .build());
        }

        log.debug("Executing Google search: query={}, maxResults={}", query, maxResults);

        return webClient.get()
                .uri(baseUrl, uriBuilder -> uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("cx", cx)
                        .queryParam("q", query)
                        .queryParam("num", Math.min(maxResults, 10))
                        .build())
                .retrieve()
                .bodyToMono(GoogleSearchApiResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> mapToSearchResponse(query, response))
                .doOnSuccess(response -> log.debug("Google search completed: {} results", response.getTotalResults()))
                .doOnError(error -> log.error("Google search failed: {}", error.getMessage()));
    }

    private SearchResponse mapToSearchResponse(String query, GoogleSearchApiResponse apiResponse) {
        long totalResults = 0;
        if (apiResponse.getSearchInformation() != null &&
                apiResponse.getSearchInformation().getTotalResults() != null) {
            try {
                totalResults = Long.parseLong(apiResponse.getSearchInformation().getTotalResults());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse total results: {}", apiResponse.getSearchInformation().getTotalResults());
            }
        }

        List<SearchResult> items = Collections.emptyList();
        if (apiResponse.getItems() != null) {
            items = apiResponse.getItems().stream()
                    .map(this::mapToSearchResult)
                    .collect(Collectors.toList());
        }

        return SearchResponse.builder()
                .query(query)
                .totalResults(totalResults)
                .items(items)
                .build();
    }

    private SearchResult mapToSearchResult(GoogleSearchApiResponse.Item item) {
        LocalDate publishedDate = extractPublishedDate(item);

        return SearchResult.builder()
                .title(item.getTitle())
                .link(item.getLink())
                .snippet(item.getSnippet())
                .displayLink(item.getDisplayLink())
                .publishedDate(publishedDate)
                .build();
    }

    private LocalDate extractPublishedDate(GoogleSearchApiResponse.Item item) {
        if (item.getPagemap() == null || item.getPagemap().getMetatags() == null) {
            return null;
        }

        return item.getPagemap().getMetatags().stream()
                .filter(metatag -> metatag.getPublishedTime() != null || metatag.getUpdatedTime() != null)
                .findFirst()
                .map(metatag -> {
                    String dateStr = metatag.getPublishedTime() != null ?
                            metatag.getPublishedTime() : metatag.getUpdatedTime();
                    return parseDate(dateStr);
                })
                .orElse(null);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ISO_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr.substring(0, Math.min(dateStr.length(), 10)), DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private Mono<SearchResponse> searchFallback(String query, Throwable throwable) {
        log.warn("Google search fallback triggered for query: {}. Error: {}", query, throwable.getMessage());
        return Mono.just(SearchResponse.builder()
                .query(query)
                .totalResults(0)
                .items(Collections.emptyList())
                .build());
    }

    @SuppressWarnings("unused")
    private Mono<SearchResponse> searchFallback(String query, int maxResults, Throwable throwable) {
        return searchFallback(query, throwable);
    }

    @Override
    public String getProviderName() {
        return "Google Custom Search";
    }
}
