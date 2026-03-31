package ru.baikalsr.backend.Applications.mapper;

import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseDetailsDto;
import ru.baikalsr.backend.Applications.entity.ArtifactRelease;

/**
 * Ручной маппер DTO для релизов приложений.
 */
@Component
public class ArtifactReleaseMapper {

    /**
     * Преобразует сущность релиза в детальное DTO.
     */
    public ArtifactReleaseDetailsDto toDetails(ArtifactRelease src) {
        if (src == null) return null;

        return new ArtifactReleaseDetailsDto(
                src.getId(),
                src.getApp().getId(),
                src.getVersionCode(),
                src.getVersionName(),
                src.getSha256(),
                src.getSizeBytes(),
                src.getStoragePath(),
                src.getStorageType(),
                src.getUploadedAt(),
                src.getUploadedBy(),
                src.getNotes()
        );
    }
}
