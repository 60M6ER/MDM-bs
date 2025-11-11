package ru.baikalsr.backend.Exchange.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record StateUpdate(
        @NotNull StateKey key,
        @NotNull JsonNode value,          // сырой JSON, хендлер сам приведёт тип
        Long observedAtEpochMs            // когда устройство зафиксировало значение
) {}
