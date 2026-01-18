package hanyang.erica.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaudeApiRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("max_tokens")
    private int maxTokens;

    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("system")
    private String system;

    @JsonProperty("messages")
    private List<Message> messages;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;
    }
}
