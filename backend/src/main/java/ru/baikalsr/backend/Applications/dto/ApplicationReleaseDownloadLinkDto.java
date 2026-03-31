package ru.baikalsr.backend.Applications.dto;

/**
 * DTO со ссылкой на скачивание файла релиза приложения.
 */
public record ApplicationReleaseDownloadLinkDto(
        Long releaseId,
        String downloadUrl
) {}
