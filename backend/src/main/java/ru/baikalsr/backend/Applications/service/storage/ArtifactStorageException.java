package ru.baikalsr.backend.Applications.service.storage;

/**
 * Runtime exception для ошибок файлового storage артефактов приложений.
 */
public class ArtifactStorageException extends RuntimeException {

    public ArtifactStorageException(String message) {
        super(message);
    }

    public ArtifactStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
