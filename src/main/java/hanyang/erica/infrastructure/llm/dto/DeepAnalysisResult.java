package hanyang.erica.infrastructure.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeepAnalysisResult {
    private String overallObservation;
    private List<AnomalyDetail> anomalies;
    private String recommendation;
    private int confidenceScore;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnomalyDetail {
        private String signalType;
        private String description;
        private int severity;
        private String evidence;
    }
}
