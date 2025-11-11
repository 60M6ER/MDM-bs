package ru.baikalsr.backend.Exchange.state;

import ru.baikalsr.backend.Exchange.dto.StateKey;

public interface StateHandler<T> {
    StateKey key();
    Class<T> type(); // во что приводим JsonNode
    void apply(String deviceId, T value, long observedAtMs);
}
