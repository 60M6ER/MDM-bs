package ru.baikalsr.backend.Device.dto;

import java.util.UUID;

public record SetKioskModeCommandRequest(
        UUID device_id,
        boolean enabled
) {}
