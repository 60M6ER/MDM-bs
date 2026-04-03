package ru.baikalsr.backend.Device.dto;

import java.util.UUID;

public record DeviceOnlineStatusDto(
        UUID deviceId,
        boolean online
) {
}
