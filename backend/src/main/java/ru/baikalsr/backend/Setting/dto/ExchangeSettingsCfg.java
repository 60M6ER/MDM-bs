package ru.baikalsr.backend.Setting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Настройки обмена данными и шаблон QR-кода")
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeSettingsCfg {
    @Schema(
            description = "Период обмена данными с устройством в секундах",
            example = "60"
    )
    private int exchangePeriodSec;

    @Schema(
            description = "Текст (JSON) для генерации QR-кода. Заполняется Android-разработчиком",
            example = "{ \"serverUrl\": \"https://mdm.example.com\" }"
    )
    private String qrPayloadText;
}
