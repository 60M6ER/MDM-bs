package ru.baikalsr.backend.Applications.service.apk;

/**
 * Минимальный набор метаданных APK, нужный backend для валидации релиза.
 */
public record ApkMetadata(
        String packageName,
        Integer versionCode,
        String versionName
) {}
