package hanyang.erica.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeApiResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private List<Content> content;

    @JsonProperty("model")
    private String model;

    @JsonProperty("stop_reason")
    private String stopReason;

    @JsonProperty("usage")
    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("input_tokens")
        private int inputTokens;

        @JsonProperty("output_tokens")
        private int outputTokens;
    }

    public String getTextContent() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.stream()
                .filter(c -> "text".equals(c.getType()))
                .map(Content::getText)
                .findFirst()
                .orElse("");
    }
}
