package ru.baikalsr.backend.Exchange.dto;

public record CommandDto(
        String key,          // тип/имя
        String payload,
        Long ttlSec,          // время жизни
        Long createdAtEpochMs
) {
}
