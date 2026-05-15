package ru.baikalsr.backend.common.dto;

import java.util.List;

public record LogSearchResultDto(
        String query,
        long totalMatches,
        boolean truncated,
        List<Long> matches
) {
}
