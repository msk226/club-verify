package hanyang.erica.infrastructure.llm;

import hanyang.erica.infrastructure.llm.dto.LlmRequest;
import hanyang.erica.infrastructure.llm.dto.LlmResponse;
import reactor.core.publisher.Mono;

public interface LlmClient {

    Mono<LlmResponse> chat(LlmRequest request);

    String getProviderName();
}
