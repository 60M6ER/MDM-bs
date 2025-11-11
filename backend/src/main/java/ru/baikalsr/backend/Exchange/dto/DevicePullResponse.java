package ru.baikalsr.backend.Exchange.dto;

import java.util.List;

public record DevicePullResponse(
        long serverTimeEpochMs,
        List<CommandDto> commands
) {
}
