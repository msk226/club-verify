package hanyang.erica.infrastructure.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmRequest {
    private String systemPrompt;
    private String userPrompt;
    private int maxTokens;
    private double temperature;
}
