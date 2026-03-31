package ru.baikalsr.backend.Applications.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Подключает бин настроек storage-слоя для артефактов приложений.
 */
@Configuration
@EnableConfigurationProperties(ArtifactStorageProperties.class)
public class ArtifactStorageConfig {
}
