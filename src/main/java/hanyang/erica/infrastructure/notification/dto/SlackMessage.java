package hanyang.erica.infrastructure.notification.dto;

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
public class SlackMessage {

    @JsonProperty("text")
    private String text;

    @JsonProperty("blocks")
    private List<Block> blocks;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Block {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private TextObject text;

        @JsonProperty("fields")
        private List<TextObject> fields;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextObject {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;
    }
}
