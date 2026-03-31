package ru.baikalsr.backend.Applications.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * Настройки локального хранилища файлов артефактов приложений.
 */
@Validated
@ConfigurationProperties(prefix = "app.artifact-storage")
public class ArtifactStorageProperties {

    /**
     * Корневая директория файлового хранилища.
     * В БД сохраняются только относительные пути внутри этого каталога.
     */
    @NotNull
    private Path rootPath;

    public Path getRootPath() {
        return rootPath;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }
}
