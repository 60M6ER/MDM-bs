package ru.baikalsr.backend.Applications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseCreateRequest;
import ru.baikalsr.backend.Applications.dto.ArtifactReleaseDetailsDto;
import ru.baikalsr.backend.Applications.entity.ArtifactApp;
import ru.baikalsr.backend.Applications.entity.ArtifactRelease;
import ru.baikalsr.backend.Applications.mapper.ArtifactReleaseMapper;
import ru.baikalsr.backend.Applications.model.StoredArtifact;
import ru.baikalsr.backend.Applications.repository.ArtifactAppRepository;
import ru.baikalsr.backend.Applications.repository.ArtifactCurrentReleaseRepository;
import ru.baikalsr.backend.Applications.repository.ArtifactReleaseRepository;
import ru.baikalsr.backend.Applications.service.apk.ApkMetadata;
import ru.baikalsr.backend.Applications.service.apk.ApkMetadataExtractor;
import ru.baikalsr.backend.Applications.service.storage.ArtifactStorageService;
import ru.baikalsr.backend.Applications.specification.ArtifactReleaseSpecifications;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

/**
 * Сервис управления релизами приложений.
 * Отвечает за создание и удаление записей в artifact_release
 * и за синхронную работу с файловым storage.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ArtifactReleaseService {

    private static final String LOCAL_STORAGE_TYPE = "LOCAL_FS";
    private static final String PENDING_STORAGE_PATH = "__pending__";
    private static final String PENDING_SHA256 = "__pending__";

    private final ArtifactAppRepository artifactAppRepository;
    private final ArtifactReleaseRepository artifactReleaseRepository;
    private final ArtifactCurrentReleaseRepository artifactCurrentReleaseRepository;
    private final ArtifactStorageService artifactStorageService;
    private final ArtifactReleaseMapper artifactReleaseMapper;
    private final ApkMetadataExtractor apkMetadataExtractor;

    /**
     * Возвращает постраничный список релизов с фильтрацией по приложению и поиском по версии.
     *
     * @param appId идентификатор приложения; если null, поиск идет по всем приложениям
     * @param search строка поиска по versionName или versionCode
     * @param pageable параметры пагинации и сортировки
     * @return страница детальных DTO релизов
     */
    public Page<ArtifactReleaseDetailsDto> findAll(Long appId, String search, Pageable pageable) {
        return artifactReleaseRepository.findAll(
                ArtifactReleaseSpecifications.withFilters(appId, search),
                pageable
        ).map(artifactReleaseMapper::toDetails);
    }

    /**
     * Возвращает детальные метаданные релиза по идентификатору.
     *
     * @param releaseId идентификатор релиза
     * @return детальное представление релиза
     * @throws ResponseStatusException если релиз не найден
     */
    public ArtifactReleaseDetailsDto getDetails(Long releaseId) {
        ArtifactRelease release = artifactReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_RELEASE_NOT_FOUND"));
        return artifactReleaseMapper.toDetails(release);
    }

    /**
     * Загружает бинарный файл релиза по идентификатору релиза.
     *
     * @param releaseId идентификатор релиза
     * @return ресурс файла из storage
     * @throws ResponseStatusException если релиз не найден
     */
    public Resource loadArtifact(Long releaseId) {
        ArtifactRelease release = artifactReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_RELEASE_NOT_FOUND"));
        return artifactStorageService.loadAsResource(release.getStoragePath());
    }

    /**
     * Формирует имя файла для скачивания, не зависящее от служебного имени в storage.
     *
     * @param releaseId идентификатор релиза
     * @return пользовательское имя файла вида NAME_VERSION_BUILD.ext
     */
    public String getDownloadFilename(Long releaseId) {
        ArtifactRelease release = artifactReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_RELEASE_NOT_FOUND"));

        String extension = extractExtension(release.getStoragePath());
        String appName = normalizeFilenamePart(release.getApp().getName(), "application");
        String versionName = normalizeFilenamePart(release.getVersionName(), "no_version");
        String versionCode = normalizeFilenamePart(String.valueOf(release.getVersionCode()), "0");

        return appName + "_" + versionName + "_" + versionCode + extension;
    }

    /**
     * Создает новый релиз приложения, сохраняет бинарный файл в storage
     * и записывает в БД только метаданные и относительный путь к файлу.
     *
     * Алгоритм:
     * 1. Проверить существование приложения.
     * 2. Извлечь packageName, versionCode и versionName из APK.
     * 3. Проверить уникальность versionCode внутри app и синхронизировать packageName приложения.
     * 4. Создать запись релиза и получить releaseId.
     * 5. Сохранить файл в storage по схеме apps/{appKey}/{releaseId}/artifact{ext}.
     * 6. Перенести вычисленные storage-метаданные в запись релиза.
     *
     * @param request данные создаваемого релиза
     * @param file бинарный файл артефакта
     * @return детальное представление созданного релиза
     */
    @Transactional
    public ArtifactReleaseDetailsDto create(ArtifactReleaseCreateRequest request, MultipartFile file) {
        ArtifactApp app = artifactAppRepository.findById(request.appId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND"));

        ApkMetadata apkMetadata = apkMetadataExtractor.extract(file);

        if (artifactReleaseRepository.existsByApp_IdAndVersionCode(app.getId(), apkMetadata.versionCode())) {
            log.warn(
                    "Rejected application release creation: duplicate versionCode, appId={}, appKey={}, versionCode={}",
                    app.getId(),
                    app.getKey(),
                    apkMetadata.versionCode()
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "APPLICATION_RELEASE_VERSION_ALREADY_EXISTS");
        }

        synchronizePackageName(app, apkMetadata.packageName());

        ArtifactRelease release = ArtifactRelease.builder()
                .app(app)
                .versionCode(apkMetadata.versionCode())
                .versionName(apkMetadata.versionName())
                .sha256(PENDING_SHA256)
                .sizeBytes(null)
                .storagePath(PENDING_STORAGE_PATH)
                .storageType(LOCAL_STORAGE_TYPE)
                .uploadedBy(request.uploadedBy())
                .notes(request.notes())
                .build();

        release = artifactReleaseRepository.saveAndFlush(release);

        StoredArtifact storedArtifact = null;
        try {
            storedArtifact = artifactStorageService.save(app.getKey(), release.getId(), file);

            release.setSha256(storedArtifact.sha256());
            release.setSizeBytes(storedArtifact.sizeBytes());
            release.setStoragePath(storedArtifact.relativePath());
            release.setStorageType(LOCAL_STORAGE_TYPE);

            ArtifactRelease savedRelease = artifactReleaseRepository.save(release);
            log.info("Created application release id={}, appId={}, versionCode={}", savedRelease.getId(), app.getId(), savedRelease.getVersionCode());
            return artifactReleaseMapper.toDetails(savedRelease);
        } catch (RuntimeException e) {
            if (storedArtifact != null) {
                try {
                    artifactStorageService.delete(storedArtifact.relativePath());
                } catch (RuntimeException cleanupError) {
                    log.warn("Failed to cleanup stored artifact after release creation error, path={}", storedArtifact.relativePath(), cleanupError);
                }
            }
            throw e;
        }
    }

    /**
     * Удаляет релиз приложения.
     * Если релиз назначен текущим в artifact_current_release, удаление запрещено.
     *
     * @param releaseId идентификатор релиза
     */
    @Transactional
    public void delete(Long releaseId) {
        ArtifactRelease release = artifactReleaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "APPLICATION_RELEASE_NOT_FOUND"));

        if (artifactCurrentReleaseRepository.findByRelease_Id(releaseId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CURRENT_APPLICATION_RELEASE_DELETE_FORBIDDEN");
        }

        artifactReleaseRepository.delete(release);
        artifactReleaseRepository.flush();

        artifactStorageService.delete(release.getStoragePath());

        log.info("Deleted application release id={}, appId={}, versionCode={}", release.getId(), release.getApp().getId(), release.getVersionCode());
    }

    private void synchronizePackageName(ArtifactApp app, String extractedPackageName) {
        if (!StringUtils.hasText(app.getPackageName())) {
            artifactAppRepository.findByPackageName(extractedPackageName)
                    .filter(existingApp -> !existingApp.getId().equals(app.getId()))
                    .ifPresent(existingApp -> {
                        log.warn(
                                "Rejected application release creation: package already assigned to another app, appId={}, appKey={}, packageName={}, existingAppId={}",
                                app.getId(),
                                app.getKey(),
                                extractedPackageName,
                                existingApp.getId()
                        );
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "APPLICATION_PACKAGE_ALREADY_EXISTS");
                    });

            app.setPackageName(extractedPackageName);
            artifactAppRepository.save(app);
            return;
        }

        if (!app.getPackageName().equals(extractedPackageName)) {
            log.warn(
                    "Rejected application release creation: package mismatch, appId={}, appKey={}, expectedPackage={}, actualPackage={}",
                    app.getId(),
                    app.getKey(),
                    app.getPackageName(),
                    extractedPackageName
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "APPLICATION_RELEASE_PACKAGE_MISMATCH");
        }
    }

    private String extractExtension(String storagePath) {
        String filename = Path.of(storagePath).getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }

    private String normalizeFilenamePart(String rawValue, String fallback) {
        if (!StringUtils.hasText(rawValue)) {
            return fallback;
        }

        String normalized = rawValue.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^\\p{L}\\p{N}._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");

        return StringUtils.hasText(normalized) ? normalized : fallback;
    }
}
