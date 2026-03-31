package ru.baikalsr.backend.Applications.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.baikalsr.backend.Applications.config.ArtifactStorageProperties;
import ru.baikalsr.backend.Applications.model.StoredArtifact;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Локальная файловая реализация storage для артефактов приложений.
 *
 * Типовой сценарий использования из ArtifactReleaseService:
 * 1. Создать запись релиза в БД и получить releaseId.
 * 2. Вызвать {@link #save(String, Long, MultipartFile)}.
 * 3. Сохранить в ArtifactRelease relativePath, sha256 и sizeBytes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalArtifactStorageService implements ArtifactStorageService {

    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[A-Za-z0-9]+");

    private final ArtifactStorageProperties properties;

    private Path rootPath;

    /**
     * Проверяет и подготавливает корневую директорию локального storage.
     */
    @PostConstruct
    public void init() {
        Path configuredRoot = properties.getRootPath();
        if (configuredRoot == null) {
            throw new ArtifactStorageException("Artifact storage rootPath is not configured");
        }

        rootPath = configuredRoot.toAbsolutePath().normalize();

        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new ArtifactStorageException("Failed to create artifact storage root directory: " + rootPath, e);
        }

        if (!Files.isDirectory(rootPath)) {
            throw new ArtifactStorageException("Artifact storage rootPath is not a directory: " + rootPath);
        }
        if (!Files.isWritable(rootPath)) {
            throw new ArtifactStorageException("Artifact storage rootPath is not writable: " + rootPath);
        }

        log.info("Artifact storage root initialized at {}", rootPath);
    }

    /**
     * Сохраняет файл релиза в локальное хранилище по схеме apps/{appKey}/{releaseId}/artifact{ext}.
     */
    @Override
    public StoredArtifact save(String appKey, Long releaseId, MultipartFile file) {
        if (releaseId == null) {
            throw new ArtifactStorageException("Release id must not be null");
        }
        if (file == null || file.isEmpty()) {
            throw new ArtifactStorageException("Artifact file must not be empty");
        }

        String safeAppKey = sanitizePathSegment(appKey, "appKey");
        String extension = extractExtension(file.getOriginalFilename());
        String storedFilename = "artifact" + extension;
        Path releaseDir = resolve(Path.of("apps", safeAppKey, String.valueOf(releaseId)));
        Path targetPath = releaseDir.resolve(storedFilename).normalize();

        ensurePathInsideRoot(targetPath);

        try {
            Files.createDirectories(releaseDir);
            Path tempFile = Files.createTempFile(releaseDir, "artifact-", ".tmp");

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream inputStream = file.getInputStream();
                     DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                    Files.copy(digestInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                moveTempFile(tempFile, targetPath);

                long sizeBytes = Files.size(targetPath);
                String relativePath = rootPath.relativize(targetPath).toString().replace('\\', '/');
                String sha256 = HexFormat.of().formatHex(digest.digest());

                return new StoredArtifact(
                        relativePath,
                        file.getOriginalFilename(),
                        storedFilename,
                        file.getContentType(),
                        sizeBytes,
                        sha256
                );
            } catch (IOException | NoSuchAlgorithmException e) {
                Files.deleteIfExists(tempFile);
                throw new ArtifactStorageException("Failed to store artifact file for release " + releaseId, e);
            }
        } catch (IOException e) {
            throw new ArtifactStorageException("Failed to prepare artifact storage directory for release " + releaseId, e);
        }
    }

    /**
     * Загружает сохраненный файл как Spring Resource по относительному пути внутри rootPath.
     */
    @Override
    public Resource loadAsResource(String relativePath) {
        Path resolvedPath = resolve(relativePath);
        if (!Files.isRegularFile(resolvedPath)) {
            throw new ArtifactStorageException("Artifact path does not point to a file: " + relativePath);
        }

        try {
            Resource resource = new UrlResource(resolvedPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ArtifactStorageException("Artifact file is not readable: " + relativePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ArtifactStorageException("Failed to load artifact resource: " + relativePath, e);
        }
    }

    /**
     * Удаляет файл по относительному пути внутри rootPath.
     */
    @Override
    public void delete(String relativePath) {
        Path resolvedPath = resolve(relativePath);
        if (Files.exists(resolvedPath) && Files.isDirectory(resolvedPath)) {
            throw new ArtifactStorageException("Artifact path does not point to a file: " + relativePath);
        }

        try {
            Files.deleteIfExists(resolvedPath);
        } catch (IOException e) {
            throw new ArtifactStorageException("Failed to delete artifact file: " + relativePath, e);
        }
    }

    /**
     * Возвращает безопасный абсолютный путь внутри корня storage для указанного относительного пути.
     */
    @Override
    public Path resolve(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new ArtifactStorageException("Relative path must not be blank");
        }

        Path candidate = Path.of(relativePath).normalize();
        if (candidate.isAbsolute()) {
            throw new ArtifactStorageException("Absolute paths are not allowed: " + relativePath);
        }
        if (candidate.getNameCount() == 0) {
            throw new ArtifactStorageException("Relative path must not point to storage root");
        }

        Path resolvedPath = rootPath.resolve(candidate).normalize();
        ensurePathInsideRoot(resolvedPath);
        return resolvedPath;
    }

    private Path resolve(Path relativePath) {
        Path normalized = relativePath.normalize();
        Path resolvedPath = rootPath.resolve(normalized).normalize();
        ensurePathInsideRoot(resolvedPath);
        return resolvedPath;
    }

    private void ensurePathInsideRoot(Path path) {
        if (!path.startsWith(rootPath)) {
            throw new ArtifactStorageException("Path escapes artifact storage root: " + path);
        }
    }

    private String sanitizePathSegment(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ArtifactStorageException(fieldName + " must not be blank");
        }

        String trimmed = value.trim();
        if (!SAFE_PATH_SEGMENT.matcher(trimmed).matches()) {
            throw new ArtifactStorageException(fieldName + " contains unsupported characters: " + value);
        }
        return trimmed;
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            return "";
        }

        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_EXTENSION.matcher(normalized).matches()) {
            throw new ArtifactStorageException("Unsupported file extension: " + originalFilename);
        }

        return "." + normalized;
    }

    private void moveTempFile(Path tempFile, Path targetPath) throws IOException {
        try {
            Files.move(
                    tempFile,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
