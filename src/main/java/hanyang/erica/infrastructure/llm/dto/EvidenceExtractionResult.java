package hanyang.erica.infrastructure.llm.dto;

import hanyang.erica.domain.evidence.entity.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceExtractionResult {
    private List<ExtractedEvidence> evidences;
    private String summary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtractedEvidence {
        private String evidenceId;
        private SourceType sourceType;
        private String url;
        private String title;
        private String snippet;
        private Integer signalStrength;
        private String signalType;
        private List<String> detectedRiskKeywords;
        private Boolean isOfficialSource;
        private Integer estimatedActivityMonths;
    }
}
