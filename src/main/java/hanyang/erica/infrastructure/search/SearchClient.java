package hanyang.erica.infrastructure.search;

import hanyang.erica.infrastructure.search.dto.SearchResponse;
import reactor.core.publisher.Mono;

public interface SearchClient {

    Mono<SearchResponse> search(String query);

    Mono<SearchResponse> search(String query, int maxResults);

    String getProviderName();
}
