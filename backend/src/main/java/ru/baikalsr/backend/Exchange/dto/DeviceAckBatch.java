package ru.baikalsr.backend.Exchange.dto;

import java.util.List;

public record DeviceAckBatch(
        String deviceId,
        String requestId,
        List<AckDto> acks,
        long receivedAtMs
) {}
