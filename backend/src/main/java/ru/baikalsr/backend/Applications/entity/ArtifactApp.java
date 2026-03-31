package ru.baikalsr.backend.Applications.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(
        name = "artifact_app",
        indexes = {
                @Index(name = "uk_artifact_app_key", columnList = "key", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtifactApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", nullable = false, length = 64, unique = true)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "app", fetch = FetchType.LAZY)
    private Set<ArtifactRelease> releases;

    @OneToOne(mappedBy = "app", fetch = FetchType.LAZY)
    private ArtifactCurrentRelease currentRelease;
}
