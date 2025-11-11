package ru.baikalsr.backend.Device.dto;

import java.util.UUID;

public record DeviceRegisterResponse(

        UUID deviceId,
        String deviceSecret,   // отдаётся один раз
        long expiresInSec      // 0 — бессрочный секрет; поле зарезервировано под будущее
) {}
