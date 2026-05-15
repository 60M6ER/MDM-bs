package ru.baikalsr.backend.common.error;

import java.util.Map;

public final class ApiErrorMessages {

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("APPLICATION_NOT_FOUND", "Приложение не найдено."),
            Map.entry("APPLICATION_KEY_ALREADY_EXISTS", "Приложение с таким ключом уже существует."),
            Map.entry("SYSTEM_APPLICATION_DELETE_FORBIDDEN", "Системное приложение нельзя удалить."),
            Map.entry("APPLICATION_RELEASE_NOT_FOUND", "Релиз приложения не найден."),
            Map.entry("APPLICATION_CURRENT_RELEASE_NOT_FOUND", "Для приложения еще не назначен текущий релиз."),
            Map.entry("APPLICATION_RELEASE_VERSION_ALREADY_EXISTS", "Релиз с таким номером сборки уже существует для этого приложения."),
            Map.entry("APPLICATION_PACKAGE_ALREADY_EXISTS", "Этот package name уже привязан к другому приложению."),
            Map.entry("APPLICATION_RELEASE_PACKAGE_MISMATCH", "У загружаемого APK другой package name, чем у выбранного приложения."),
            Map.entry("APPLICATION_RELEASE_BELONGS_TO_ANOTHER_APP", "Выбранный релиз принадлежит другому приложению."),
            Map.entry("APPLICATION_RESET_FORBIDDEN", "Обнуление приложения разрешено только резервному администратору."),
            Map.entry("CURRENT_APPLICATION_RELEASE_DELETE_FORBIDDEN", "Нельзя удалить релиз, который назначен текущим."),
            Map.entry("APPLICATION_RELEASE_INVALID_APK", "Не удалось прочитать APK-файл. Проверьте, что загружается корректный APK."),
            Map.entry("APPLICATION_RELEASE_APK_PACKAGE_NAME_NOT_FOUND", "В APK не найден package name."),
            Map.entry("APPLICATION_RELEASE_APK_VERSION_CODE_NOT_FOUND", "В APK не найден versionCode."),
            Map.entry("PUBLIC_BASIC_URL_NOT_CONFIGURED", "На сервере не настроен PUBLIC_BASIC_URL."),
            Map.entry("REG_KEY_INVALID", "Ключ регистрации устройства недействителен."),
            Map.entry("LOG_VIEWER_READ_FAILED", "Не удалось прочитать файл логов на сервере."),
            Map.entry("INVALID_LOG_CHUNK_RANGE", "Запрошен некорректный диапазон строк логов.")
    );

    private ApiErrorMessages() {
    }

    public static String resolve(String code, String fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        return MESSAGES.getOrDefault(code, fallback);
    }
}
