package hanyang.erica.api.dto.request;

import hanyang.erica.domain.club.entity.ClubCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubApplicationRequest {

    @NotBlank(message = "동아리명은 필수입니다")
    @Size(max = 100, message = "동아리명은 100자 이하여야 합니다")
    private String clubName;

    @NotNull(message = "동아리 분류는 필수입니다")
    private ClubCategory category;

    @Size(max = 50, message = "세부 카테고리는 50자 이하여야 합니다")
    private String subCategory;

    @Size(max = 200, message = "한 줄 소개는 200자 이하여야 합니다")
    private String shortDescription;

    private String fullDescription;

    @Size(max = 100, message = "이메일 도메인은 100자 이하여야 합니다")
    private String representativeEmailDomain;
}
