package ru.baikalsr.backend.Applications.service.apk;

import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Извлекает служебные метаданные из APK-файла.
 */
@Component
@Slf4j
public class ApkMetadataExtractor {

    public ApkMetadata extract(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("artifact-release-", ".apk");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return extract(tempFile);
        } catch (IOException e) {
            log.warn("Failed to prepare temp file for APK metadata extraction", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APPLICATION_RELEASE_INVALID_APK");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to cleanup temp APK file {}", tempFile, e);
                }
            }
        }
    }

    public ApkMetadata extract(Path apkPath) {
        try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
            ApkMeta apkMeta = apkFile.getApkMeta();
            String packageName = apkMeta != null ? apkMeta.getPackageName() : null;
            Long versionCode = apkMeta != null ? apkMeta.getVersionCode() : null;
            String versionName = apkMeta != null ? apkMeta.getVersionName() : null;

            if (!StringUtils.hasText(packageName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APPLICATION_RELEASE_APK_PACKAGE_NAME_NOT_FOUND");
            }
            if (versionCode == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APPLICATION_RELEASE_APK_VERSION_CODE_NOT_FOUND");
            }

            return new ApkMetadata(
                    packageName.trim(),
                    Math.toIntExact(versionCode),
                    StringUtils.hasText(versionName) ? versionName.trim() : null
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to extract APK metadata from {}", apkPath, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APPLICATION_RELEASE_INVALID_APK");
        }
    }
}
