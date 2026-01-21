package ru.baikalsr.backend.Exchange.dto;

import ru.baikalsr.backend.Device.enums.DeviceCommand;

public record CommandDto(
        DeviceCommand key,          // тип/имя
        Object payload,
        Long ttlSec,          // время жизни
        Long createdAtEpochMs
) {
}
