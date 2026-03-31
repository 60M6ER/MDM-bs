package ru.baikalsr.backend.Applications.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "artifact_current_release",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_artifact_current_release_release_id",
                        columnNames = "release_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtifactCurrentRelease {

    @Id
    @Column(name = "app_id", nullable = false)
    private Long appId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "app_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_artifact_current_release_app")
    )
    private ArtifactApp app;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "release_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_artifact_current_release_release")
    )
    private ArtifactRelease release;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", length = 128)
    private String assignedBy;
}
