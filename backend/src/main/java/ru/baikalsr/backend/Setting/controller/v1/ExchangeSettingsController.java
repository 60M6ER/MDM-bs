package ru.baikalsr.backend.Setting.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.baikalsr.backend.Setting.dto.AuthSettingsCfg;
import ru.baikalsr.backend.Setting.dto.ExchangeSettingsCfg;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.service.SettingsService;

@RestController
@RequestMapping("/api/v1/settings/exchange_settings")
@RequiredArgsConstructor
@Tag(name = "Settings: Exchange", description = "Настройки обмена данными с устройствами.")
public class ExchangeSettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(
            summary = "Получить EXCHANGE_SETTINGS",
            description = "Возвращает текущие настройки обмена и шаблон QR-кода. Если не заданы — вернётся дефолт."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ExchangeSettingsCfg.class))
    )
    public ResponseEntity<ExchangeSettingsCfg> getExchangeSettings() {
        ExchangeSettingsCfg cfg =
                settingsService.get(SettingGroup.EXCHANGE_SETTINGS, ExchangeSettingsCfg.class);
        return ResponseEntity.ok(cfg);
    }

    @PutMapping
    @Operation(
            summary = "Сохранить EXCHANGE_SETTINGS",
            description = "Сохраняет период обмена и текст для QR-кода"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сохранено",
            content = @Content(schema = @Schema(implementation = ExchangeSettingsCfg.class))
    )
    public ResponseEntity<ExchangeSettingsCfg> saveExchangeSettings(
            @RequestBody ExchangeSettingsCfg body
    ) {
        settingsService.save(SettingGroup.EXCHANGE_SETTINGS, body);
        return ResponseEntity.ok(body);
    }
}
