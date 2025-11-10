package ru.baikalsr.backend.Device.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceDetailsDto(
        // Identity
        UUID deviceId,
        String deviceName,
        String serialNumber,
        String inventoryNumber,
        String status,
        String model,
        String manufacturer,

        // Lifecycle
        Instant enrolledAt,
        Instant deactivatedAt,
        Instant deviceCreatedAt,
        Instant deviceUpdatedAt,

        // State
        Instant stateLastSeenAt,
        Instant stateUpdatedAt,
        Boolean online,
        Boolean charging,
        Short batteryLevel,
        String osVersion,
        String appVersion,
        String networkType,
        String wifiSsid,
        String ipAddress,
        Long storageTotalMb,
        Long storageFreeMb,
        Double cpuTempC,

        // Owner
        String ownerDisplay,
        UUID ownerUserId,
        Instant ownerAssignedAt,

        // Location
        Double lat,
        Double lon,
        Double accuracyM,
        Double altitudeM,
        Double speedMps,
        Double headingDeg,
        String locationSource,
        Boolean locationIsMock,
        Instant locationTs,
        Instant locationReceivedAt,

        // Department
        UUID departmentId,
        Instant departmentAssignedAt
) {}
