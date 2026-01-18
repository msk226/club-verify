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
public class GeminiApiRequest {

    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("systemInstruction")
    private Content systemInstruction;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Part {
        @JsonProperty("text")
        private String text;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerationConfig {
        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("maxOutputTokens")
        private Integer maxOutputTokens;
    }
}
