package ru.baikalsr.backend.Device.dto;

import ru.baikalsr.backend.Device.enums.DeviceEvents;


import java.time.Instant;
import java.util.UUID;

public record DeviceEventDto(
        UUID id,
        DeviceEvents event,
        String details,
        Instant occurredAt
) {
}
