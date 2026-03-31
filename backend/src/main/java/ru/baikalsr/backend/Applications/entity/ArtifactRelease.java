package ru.baikalsr.backend.Applications.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "artifact_release",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_artifact_release_app_version_code",
                        columnNames = {"app_id", "version_code"}
                )
        },
        indexes = {
                @Index(name = "idx_artifact_release_app_id", columnList = "app_id"),
                @Index(name = "idx_artifact_release_app_id_version_code", columnList = "app_id, version_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtifactRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "app_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_artifact_release_app")
    )
    private ArtifactApp app;

    @Column(name = "version_code", nullable = false)
    private Integer versionCode;

    @Column(name = "version_name", length = 64)
    private String versionName;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "storage_type", nullable = false, length = 32)
    private String storageType;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "uploaded_by", length = 128)
    private String uploadedBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @OneToOne(mappedBy = "release", fetch = FetchType.LAZY)
    private ArtifactCurrentRelease currentAssignment;
}
