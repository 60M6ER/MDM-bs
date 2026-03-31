package ru.baikalsr.backend.Setting.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.baikalsr.backend.Setting.dto.PublicBasicUrlCfg;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.service.SettingsService;

@RestController
@RequestMapping("/api/v1/settings/public_basic_url")
@RequiredArgsConstructor
@Tag(name = "Settings: Public Basic URL", description = "Управление базовым публичным URL backend для построения абсолютных ссылок.")
public class PublicBasicUrlSettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(
            summary = "Получить PUBLIC_BASIC_URL",
            description = "Возвращает публичный базовый URL backend. Если настройка еще не задана, вернется пустая строка."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PublicBasicUrlCfg.class))
    )
    public ResponseEntity<PublicBasicUrlCfg> getPublicBasicUrl() {
        PublicBasicUrlCfg cfg = settingsService.get(SettingGroup.PUBLIC_BASIC_URL, PublicBasicUrlCfg.class);
        return ResponseEntity.ok(cfg);
    }

    @PutMapping
    @Operation(
            summary = "Сохранить PUBLIC_BASIC_URL",
            description = "Сохраняет публичный базовый URL backend для построения абсолютных ссылок."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сохранено",
            content = @Content(schema = @Schema(implementation = PublicBasicUrlCfg.class))
    )
    public ResponseEntity<PublicBasicUrlCfg> savePublicBasicUrl(@RequestBody PublicBasicUrlCfg body) {
        settingsService.save(SettingGroup.PUBLIC_BASIC_URL, body);
        return ResponseEntity.ok(body);
    }
}
