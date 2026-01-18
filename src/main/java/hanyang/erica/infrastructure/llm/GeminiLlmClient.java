package hanyang.erica.infrastructure.llm;

import hanyang.erica.infrastructure.llm.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@Primary
public class GeminiLlmClient implements LlmClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeout;

    public GeminiLlmClient(
            WebClient webClient,
            @Value("${external.gemini.api-key:}") String apiKey,
            @Value("${external.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${external.gemini.model:gemini-1.5-flash}") String model,
            @Value("${external.gemini.timeout:60000}") int timeout) {
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
            log.warn("Gemini API key not configured. Returning failure response.");
            return Mono.just(LlmResponse.failure("API key not configured"));
        }

        GeminiApiRequest apiRequest = buildApiRequest(request);
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        log.debug("Sending request to Gemini API: model={}, maxTokens={}",
                model, request.getMaxTokens());

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(apiRequest)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Gemini API error response: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "Gemini API error: " + response.statusCode() + " - " + body)));
                    }
                    return response.bodyToMono(GeminiApiResponse.class);
                })
                .timeout(Duration.ofMillis(timeout))
                .map(this::mapToLlmResponse)
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        log.debug("Gemini API response received: inputTokens={}, outputTokens={}",
                                response.getInputTokens(), response.getOutputTokens());
                    }
                })
                .doOnError(error -> log.error("Gemini API call failed: {}", error.getMessage()));
    }

    private GeminiApiRequest buildApiRequest(LlmRequest request) {
        GeminiApiRequest.Content systemContent = null;
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            systemContent = GeminiApiRequest.Content.builder()
                    .parts(List.of(GeminiApiRequest.Part.builder()
                            .text(request.getSystemPrompt())
                            .build()))
                    .build();
        }

        GeminiApiRequest.Content userContent = GeminiApiRequest.Content.builder()
                .role("user")
                .parts(List.of(GeminiApiRequest.Part.builder()
                        .text(request.getUserPrompt())
                        .build()))
                .build();

        return GeminiApiRequest.builder()
                .contents(List.of(userContent))
                .systemInstruction(systemContent)
                .generationConfig(GeminiApiRequest.GenerationConfig.builder()
                        .temperature(request.getTemperature())
                        .maxOutputTokens(request.getMaxTokens() > 0 ? request.getMaxTokens() : 4096)
                        .build())
                .build();
    }

    private LlmResponse mapToLlmResponse(GeminiApiResponse apiResponse) {
        String content = apiResponse.getTextContent();
        int inputTokens = apiResponse.getUsageMetadata() != null ?
                apiResponse.getUsageMetadata().getPromptTokenCount() : 0;
        int outputTokens = apiResponse.getUsageMetadata() != null ?
                apiResponse.getUsageMetadata().getCandidatesTokenCount() : 0;

        return LlmResponse.success(content, inputTokens, outputTokens);
    }

    @SuppressWarnings("unused")
    private Mono<LlmResponse> chatFallback(LlmRequest request, Throwable throwable) {
        log.warn("Gemini API fallback triggered. Error: {}", throwable.getMessage());
        return Mono.just(LlmResponse.failure("LLM service unavailable: " + throwable.getMessage()));
    }

    @Override
    public String getProviderName() {
        return "Gemini (Google)";
    }
}
