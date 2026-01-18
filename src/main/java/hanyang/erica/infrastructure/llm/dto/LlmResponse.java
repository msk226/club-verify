package hanyang.erica.infrastructure.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmResponse {
    private String content;
    private int inputTokens;
    private int outputTokens;
    private boolean success;
    private String errorMessage;

    public static LlmResponse success(String content, int inputTokens, int outputTokens) {
        return LlmResponse.builder()
                .content(content)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .success(true)
                .build();
    }

    public static LlmResponse failure(String errorMessage) {
        return LlmResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
