package ru.baikalsr.backend.Exchange.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import ru.baikalsr.backend.Device.entity.DeviceEvent;

public record EventItem(
        @NotNull DeviceEvent type,
        @NotNull JsonNode payload,        // детализация события (строго в хендлере события)
        @NotNull Long occurredAtEpochMs
) {
}
