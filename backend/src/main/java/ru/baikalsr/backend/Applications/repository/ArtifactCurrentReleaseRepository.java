package ru.baikalsr.backend.Applications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Applications.entity.ArtifactCurrentRelease;

import java.util.Optional;

public interface ArtifactCurrentReleaseRepository extends JpaRepository<ArtifactCurrentRelease, Long> {

    Optional<ArtifactCurrentRelease> findByAppId(Long appId);

    Optional<ArtifactCurrentRelease> findByRelease_Id(Long releaseId);

    Optional<ArtifactCurrentRelease> findByApp_Key(String appKey);
}
