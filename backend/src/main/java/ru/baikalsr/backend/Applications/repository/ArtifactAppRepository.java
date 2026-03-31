package ru.baikalsr.backend.Applications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Applications.entity.ArtifactApp;

import java.util.List;
import java.util.Optional;

public interface ArtifactAppRepository extends JpaRepository<ArtifactApp, Long> {

    Optional<ArtifactApp> findByKey(String key);

    Optional<ArtifactApp> findByPackageName(String packageName);

    boolean existsByKey(String key);

    List<ArtifactApp> findByActiveTrueOrderByNameAsc();
}
