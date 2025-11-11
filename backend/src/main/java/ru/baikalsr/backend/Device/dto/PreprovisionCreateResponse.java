package ru.baikalsr.backend.Device.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PreprovisionCreateResponse(
        UUID preDeviceId,
        String regKey,
        Instant expiresAtUtc,
        Map<String, Object> qrPayload
) {}
