package hanyang.erica.infrastructure.llm;

import hanyang.erica.infrastructure.llm.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class ClaudeLlmClient implements LlmClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeout;

    public ClaudeLlmClient(
            WebClient webClient,
            @Value("${external.claude.api-key:}") String apiKey,
            @Value("${external.claude.base-url:https://api.anthropic.com/v1}") String baseUrl,
            @Value("${external.claude.model:claude-sonnet-4-20250514}") String model,
            @Value("${external.claude.timeout:60000}") int timeout) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    @Retry(name = "llmApi", fallbackMethod = "chatFallback")
    @CircuitBreaker(name = "llmApi", fallbackMethod = "chatFallback")
    public Mono<LlmResponse> chat(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Claude API key not configured. Returning failure response.");
            return Mono.just(LlmResponse.failure("API key not configured"));
        }

        ClaudeApiRequest apiRequest = buildApiRequest(request);

        log.debug("Sending request to Claude API: model={}, maxTokens={}",
                apiRequest.getModel(), apiRequest.getMaxTokens());

        return webClient.post()
                .uri(baseUrl + "/messages")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .bodyValue(apiRequest)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Claude API error response: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "Claude API error: " + response.statusCode() + " - " + body)));
                    }
                    return response.bodyToMono(ClaudeApiResponse.class);
                })
                .timeout(Duration.ofMillis(timeout))
                .map(this::mapToLlmResponse)
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        log.debug("Claude API response received: inputTokens={}, outputTokens={}",
                                response.getInputTokens(), response.getOutputTokens());
                    }
                })
                .doOnError(error -> log.error("Claude API call failed: {}", error.getMessage()));
    }

    private ClaudeApiRequest buildApiRequest(LlmRequest request) {
        return ClaudeApiRequest.builder()
                .model(model)
                .maxTokens(request.getMaxTokens() > 0 ? request.getMaxTokens() : 4096)
                .temperature(request.getTemperature())
                .system(request.getSystemPrompt())
                .messages(List.of(
                        ClaudeApiRequest.Message.builder()
                                .role("user")
                                .content(request.getUserPrompt())
                                .build()
                ))
                .build();
    }

    private LlmResponse mapToLlmResponse(ClaudeApiResponse apiResponse) {
        String content = apiResponse.getTextContent();
        int inputTokens = apiResponse.getUsage() != null ? apiResponse.getUsage().getInputTokens() : 0;
        int outputTokens = apiResponse.getUsage() != null ? apiResponse.getUsage().getOutputTokens() : 0;

        return LlmResponse.success(content, inputTokens, outputTokens);
    }

    @SuppressWarnings("unused")
    private Mono<LlmResponse> chatFallback(LlmRequest request, Throwable throwable) {
        log.warn("Claude API fallback triggered. Error: {}", throwable.getMessage());
        return Mono.just(LlmResponse.failure("LLM service unavailable: " + throwable.getMessage()));
    }

    @Override
    public String getProviderName() {
        return "Claude (Anthropic)";
    }
}
