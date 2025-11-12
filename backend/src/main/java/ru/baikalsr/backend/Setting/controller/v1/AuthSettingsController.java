package ru.baikalsr.backend.Setting.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.baikalsr.backend.Setting.dto.AuthSettingsCfg;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.service.SettingsService;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

@RestController
@RequestMapping("/api/v1/settings/eauth")
@RequiredArgsConstructor
@Tag(name = "Settings: Auth", description = "Управление настройками централизованной авторизации (AD/LDAP/OIDC)")
public class AuthSettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(
            summary = "Получить AUTH_SETTINGS",
            description = "Возвращает текущую группу AUTH_SETTINGS. Если не настроена — вернётся дефолт (внешняя авторизация отключена)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = AuthSettingsCfg.class))
    )
    public ResponseEntity<AuthSettingsCfg> getAuthSettings() {
        AuthSettingsCfg cfg = settingsService.get(SettingGroup.AUTH_SETTINGS, AuthSettingsCfg.class);
        return ResponseEntity.ok(cfg);
    }

    @PutMapping
    @Operation(
            summary = "Сохранить AUTH_SETTINGS",
            description = "Сохраняет переданную конфигурацию AUTH_SETTINGS в БД и публикует событие для горячего применения."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сохранено",
            content = @Content(schema = @Schema(implementation = AuthSettingsCfg.class))
    )
    public ResponseEntity<AuthSettingsCfg> saveAuthSettings(@RequestBody AuthSettingsCfg body) {
        settingsService.save(SettingGroup.AUTH_SETTINGS, body);
        return ResponseEntity.ok(body);
    }

    /**
     * Быстрая проверка доступности LDAP/AD по данным из тела запроса (без сохранения).
     * Позволяет проверить корректность настроек до их применения.
     */
    @PostMapping("/test")
    @Operation(
            summary = "Проверка доступности (по телу запроса)",
            description = "Открывает TCP-соединение к первому LDAP/AD URL из переданных настроек (без bind и без сохранения)."
    )
    public ResponseEntity<TestResult> testConnectivity(
            @RequestBody AuthSettingsCfg cfg,
            @RequestParam(name = "timeoutMs", required = false) Integer timeoutMs
    ) {
        int to = timeoutMs == null ? 3000 : Math.max(500, timeoutMs);
        try {
            if (cfg == null) {
                return ResponseEntity.ok(TestResult.fail("Пустое тело запроса"));
            }

            String urlStr = null;
            if (cfg.ad() != null && cfg.ad().urls() != null && !cfg.ad().urls().isEmpty()) {
                urlStr = cfg.ad().urls().get(0);
            } else if (cfg.ldap() != null && cfg.ldap().url() != null) {
                urlStr = cfg.ldap().url();
            }
            if (urlStr == null || urlStr.isBlank()) {
                return ResponseEntity.ok(TestResult.fail("Не задан URL для подключения (ad.urls или ldap.url)"));
            }

            // URL не умеет ldap:// — заменяем схему для парсинга
            String adjustedUrl = urlStr.startsWith("ldaps://")
                    ? urlStr.replaceFirst("^ldaps://", "https://")
                    : urlStr.replaceFirst("^ldap://", "http://");

            URL u = new URL(adjustedUrl);
            String host = u.getHost();
            int port = u.getPort();
            if (port < 0) port = urlStr.startsWith("ldaps://") ? 636 : 389;

            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), to);
            }
            return ResponseEntity.ok(TestResult.ok("TCP connect OK: " + host + ":" + port));
        } catch (Exception e) {
            return ResponseEntity.ok(TestResult.fail("TCP connect FAIL: " + e.getMessage()));
        }
    }

    // --- DTO для результата теста ---
    public record TestResult(boolean ok, String message) {
        public static TestResult ok(String m) { return new TestResult(true, m); }
        public static TestResult fail(String m) { return new TestResult(false, m); }
    }
}
