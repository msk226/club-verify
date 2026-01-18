package hanyang.erica.infrastructure.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private String title;
    private String link;
    private String snippet;
    private String displayLink;
    private LocalDate publishedDate;
}
