package hanyang.erica.infrastructure.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    private String query;
    private long totalResults;
    private List<SearchResult> items;
}
