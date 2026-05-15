package ru.baikalsr.backend.common.dto;

public record LogLineDto(
        long lineNumber,
        String text
) {
}
