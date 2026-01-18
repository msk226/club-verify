package hanyang.erica.infrastructure.search.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSearchApiResponse {

    @JsonProperty("searchInformation")
    private SearchInformation searchInformation;

    @JsonProperty("items")
    private List<Item> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchInformation {
        @JsonProperty("totalResults")
        private String totalResults;

        @JsonProperty("searchTime")
        private Double searchTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("title")
        private String title;

        @JsonProperty("link")
        private String link;

        @JsonProperty("displayLink")
        private String displayLink;

        @JsonProperty("snippet")
        private String snippet;

        @JsonProperty("pagemap")
        private Pagemap pagemap;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagemap {
        @JsonProperty("metatags")
        private List<Metatag> metatags;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metatag {
        @JsonProperty("article:published_time")
        private String publishedTime;

        @JsonProperty("og:updated_time")
        private String updatedTime;
    }
}
