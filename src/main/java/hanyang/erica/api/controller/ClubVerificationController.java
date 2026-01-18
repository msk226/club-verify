package hanyang.erica.api.controller;

import hanyang.erica.api.dto.request.ClubApplicationRequest;
import hanyang.erica.api.dto.response.VerificationResponse;
import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.VerificationStatus;
import hanyang.erica.domain.club.repository.ClubApplicationRepository;
import hanyang.erica.domain.verification.entity.VerificationResult;
import hanyang.erica.domain.verification.service.VerificationOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubVerificationController {

    private final ClubApplicationRepository clubApplicationRepository;
    private final VerificationOrchestrator verificationOrchestrator;

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyClub(
            @Valid @RequestBody ClubApplicationRequest request) {

        log.info("Received verification request for club: {}", request.getClubName());

        ClubApplication application = ClubApplication.builder()
                .clubName(request.getClubName())
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .representativeEmailDomain(request.getRepresentativeEmailDomain())
                .status(VerificationStatus.PENDING)
                .build();

        application = clubApplicationRepository.save(application);
        log.info("Club application saved with ID: {}", application.getId());

        verificationOrchestrator.startVerification(application.getId());

        application.updateStatus(VerificationStatus.VERIFICATION_IN_PROGRESS);
        clubApplicationRepository.save(application);

        VerificationResponse response = VerificationResponse.pending(application);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{applicationId}/verification")
    public ResponseEntity<VerificationResponse> getVerificationResult(
            @PathVariable Long applicationId) {

        log.info("Fetching verification result for application: {}", applicationId);

        ClubApplication application = clubApplicationRepository.findById(applicationId)
                .orElse(null);

        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(VerificationResponse.notFound(applicationId));
        }

        VerificationResult result = verificationOrchestrator.getVerificationResult(applicationId);

        if (result == null) {
            return ResponseEntity.ok(VerificationResponse.pending(application));
        }

        return ResponseEntity.ok(VerificationResponse.fromResult(application, result));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ClubApplicationDetailResponse> getApplication(
            @PathVariable Long applicationId) {

        return clubApplicationRepository.findById(applicationId)
                .map(app -> ResponseEntity.ok(ClubApplicationDetailResponse.from(app)))
                .orElse(ResponseEntity.notFound().build());
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ClubApplicationDetailResponse {
        private Long id;
        private String clubName;
        private String category;
        private String subCategory;
        private String shortDescription;
        private String fullDescription;
        private String representativeEmailDomain;
        private String status;
        private String statusDescription;

        public static ClubApplicationDetailResponse from(ClubApplication app) {
            return ClubApplicationDetailResponse.builder()
                    .id(app.getId())
                    .clubName(app.getClubName())
                    .category(app.getCategory().name())
                    .subCategory(app.getSubCategory())
                    .shortDescription(app.getShortDescription())
                    .fullDescription(app.getFullDescription())
                    .representativeEmailDomain(app.getRepresentativeEmailDomain())
                    .status(app.getStatus().name())
                    .statusDescription(app.getStatus().getDescription())
                    .build();
        }
    }
}
