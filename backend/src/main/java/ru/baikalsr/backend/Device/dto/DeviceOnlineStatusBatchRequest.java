package ru.baikalsr.backend.Device.dto;

import java.util.List;
import java.util.UUID;

public record DeviceOnlineStatusBatchRequest(
        List<UUID> deviceIds
) {
}
