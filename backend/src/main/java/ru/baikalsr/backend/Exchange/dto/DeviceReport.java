package ru.baikalsr.backend.Exchange.dto;

public record DeviceReport(
        String deviceId,
        String requestId,
        DevicePullRequest payload,
        long receivedAtMs
) {}
