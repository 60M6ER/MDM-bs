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
        Boolean isKioskMode,

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
) {
    public DeviceDetailsDto withOnline(Boolean online) {
        return new DeviceDetailsDto(
                deviceId,
                deviceName,
                serialNumber,
                inventoryNumber,
                status,
                model,
                manufacturer,
                enrolledAt,
                deactivatedAt,
                deviceCreatedAt,
                deviceUpdatedAt,
                stateLastSeenAt,
                stateUpdatedAt,
                online,
                charging,
                batteryLevel,
                osVersion,
                appVersion,
                networkType,
                wifiSsid,
                ipAddress,
                storageTotalMb,
                storageFreeMb,
                cpuTempC,
                isKioskMode,
                ownerDisplay,
                ownerUserId,
                ownerAssignedAt,
                lat,
                lon,
                accuracyM,
                altitudeM,
                speedMps,
                headingDeg,
                locationSource,
                locationIsMock,
                locationTs,
                locationReceivedAt,
                departmentId,
                departmentAssignedAt
        );
    }
}
