package hanyang.erica.domain.evidence.entity;

import hanyang.erica.domain.club.entity.ClubApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvidenceCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String evidenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_application_id", nullable = false)
    private ClubApplication clubApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    @Column(length = 2048)
    private String url;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String snippet;

    private LocalDate publishedAt;

    @Embedded
    private EvidenceSignal signals;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
