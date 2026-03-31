package ru.baikalsr.backend.Applications.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ru.baikalsr.backend.Applications.model.StoredArtifact;

import java.nio.file.Path;

/**
 * Абстракция хранения бинарных файлов релизов приложений.
 * Бизнес-сервис релизов должен сохранять в БД только метаданные и relativePath,
 * который возвращается из {@link #save(String, Long, MultipartFile)}.
 */
public interface ArtifactStorageService {

    /**
     * Сохраняет файл релиза в storage и возвращает его вычисленные метаданные.
     */
    StoredArtifact save(String appKey, Long releaseId, MultipartFile file);

    /**
     * Загружает ранее сохраненный файл как Spring Resource по относительному пути.
     */
    Resource loadAsResource(String relativePath);

    /**
     * Удаляет файл по относительному пути.
     * Если файла нет, операция считается успешной.
     */
    void delete(String relativePath);

    /**
     * Разрешает относительный путь в безопасный абсолютный путь внутри rootPath.
     */
    Path resolve(String relativePath);
}
