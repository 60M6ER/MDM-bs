package ru.baikalsr.backend.Exchange.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DevicePullRequest(
        @NotNull Long deviceTimeEpochMs,
        @NotNull List<StateUpdate> states,
        @NotNull List<EventItem> events
) {
    public DevicePullRequest {
        if (states == null) states = List.of();
        if (events == null) events = List.of();
    }
}
