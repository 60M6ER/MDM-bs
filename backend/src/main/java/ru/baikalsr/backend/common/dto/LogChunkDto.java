package ru.baikalsr.backend.common.dto;

import java.util.List;

public record LogChunkDto(
        long totalLines,
        boolean hasOlder,
        boolean hasNewer,
        List<LogLineDto> lines
) {
}
