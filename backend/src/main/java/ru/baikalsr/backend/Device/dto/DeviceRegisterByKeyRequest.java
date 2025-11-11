package ru.baikalsr.backend.Device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeviceRegisterByKeyRequest(
        @NotNull UUID preDeviceId,
        @NotBlank String regKey,

        @NotBlank String manufacturer,
        String model,
        String osVersion,
        String appVersion,

        @NotBlank String serial        // серийный номер устройства
) {}
