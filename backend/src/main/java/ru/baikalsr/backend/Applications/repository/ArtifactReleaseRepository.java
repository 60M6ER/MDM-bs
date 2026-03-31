package ru.baikalsr.backend.Applications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.baikalsr.backend.Applications.entity.ArtifactRelease;

import java.util.List;
import java.util.Optional;

public interface ArtifactReleaseRepository extends JpaRepository<ArtifactRelease, Long>, JpaSpecificationExecutor<ArtifactRelease> {

    List<ArtifactRelease> findByApp_IdOrderByVersionCodeDesc(Long appId);

    Optional<ArtifactRelease> findByApp_IdAndVersionCode(Long appId, Integer versionCode);

    Optional<ArtifactRelease> findFirstByApp_IdOrderByVersionCodeDesc(Long appId);

    boolean existsByApp_IdAndVersionCode(Long appId, Integer versionCode);
}
