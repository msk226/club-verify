package hanyang.erica.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanyang.erica.api.dto.request.ClubApplicationRequest;
import hanyang.erica.domain.club.entity.ClubApplication;
import hanyang.erica.domain.club.entity.ClubCategory;
import hanyang.erica.domain.club.entity.VerificationStatus;
import hanyang.erica.domain.club.repository.ClubApplicationRepository;
import hanyang.erica.domain.verification.entity.RiskLevel;
import hanyang.erica.domain.verification.entity.VerificationResult;
import hanyang.erica.domain.verification.service.VerificationOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClubVerificationController.class)
class ClubVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClubApplicationRepository clubApplicationRepository;

    @MockitoBean
    private VerificationOrchestrator verificationOrchestrator;

    @Test
    @DisplayName("POST /api/v1/clubs/verify - 검증 요청 성공 시 202 반환")
    void shouldReturn202WhenVerificationRequestIsAccepted() throws Exception {
        ClubApplicationRequest request = ClubApplicationRequest.builder()
                .clubName("한양밴드")
                .category(ClubCategory.CENTRAL)
                .subCategory("음악")
                .shortDescription("음악을 사랑하는 학생들의 밴드 동아리")
                .build();

        ClubApplication savedApplication = ClubApplication.builder()
                .clubName("한양밴드")
                .category(ClubCategory.CENTRAL)
                .status(VerificationStatus.PENDING)
                .build();

        when(clubApplicationRepository.save(any(ClubApplication.class))).thenReturn(savedApplication);
        doNothing().when(verificationOrchestrator).startVerification(anyLong());

        mockMvc.perform(post("/api/v1/clubs/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.clubName").value("한양밴드"))
                .andExpect(jsonPath("$.status").value("VERIFICATION_IN_PROGRESS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/clubs/verify - 필수 필드 누락 시 400 반환")
    void shouldReturn400WhenRequiredFieldIsMissing() throws Exception {
        ClubApplicationRequest request = ClubApplicationRequest.builder()
                .subCategory("음악")
                .build();

        mockMvc.perform(post("/api/v1/clubs/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("GET /api/v1/clubs/{id}/verification - 검증 결과 조회 성공")
    void shouldReturnVerificationResultWhenExists() throws Exception {
        Long applicationId = 1L;

        ClubApplication application = ClubApplication.builder()
                .clubName("한양밴드")
                .category(ClubCategory.CENTRAL)
                .status(VerificationStatus.AUTO_APPROVED)
                .build();

        VerificationResult result = VerificationResult.builder()
                .clubApplication(application)
                .totalRiskScore(-60)
                .riskLevel(RiskLevel.AUTO_APPROVE)
                .ruleBasedScoreBreakdown("{}")
                .build();

        when(clubApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(verificationOrchestrator.getVerificationResult(applicationId)).thenReturn(result);

        mockMvc.perform(get("/api/v1/clubs/{applicationId}/verification", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("한양밴드"))
                .andExpect(jsonPath("$.riskLevel").value("AUTO_APPROVE"));
    }

    @Test
    @DisplayName("GET /api/v1/clubs/{id}/verification - 존재하지 않는 신청 시 404 반환")
    void shouldReturn404WhenApplicationNotFound() throws Exception {
        Long applicationId = 999L;

        when(clubApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clubs/{applicationId}/verification", applicationId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/clubs/{id}/verification - 검증 진행 중이면 pending 응답 반환")
    void shouldReturnPendingWhenVerificationInProgress() throws Exception {
        Long applicationId = 1L;

        ClubApplication application = ClubApplication.builder()
                .clubName("한양밴드")
                .category(ClubCategory.CENTRAL)
                .status(VerificationStatus.VERIFICATION_IN_PROGRESS)
                .build();

        when(clubApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(verificationOrchestrator.getVerificationResult(applicationId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/clubs/{applicationId}/verification", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFICATION_IN_PROGRESS"))
                .andExpect(jsonPath("$.message").exists());
    }
}
