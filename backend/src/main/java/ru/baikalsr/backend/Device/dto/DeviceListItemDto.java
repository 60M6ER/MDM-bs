package ru.baikalsr.backend.Device.dto;

import java.util.UUID;

public record DeviceListItemDto(
        UUID deviceId,
        String inventoryNumber,
        String model,
        String manufacturer,
        String deviceName,
        String serialNumber
) {}
