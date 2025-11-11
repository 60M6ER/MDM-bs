package ru.baikalsr.backend.Exchange.service;

import ru.baikalsr.backend.Exchange.dto.AckRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullResponse;

public interface ExchangeService {
    DevicePullResponse pull(String deviceId, String requestId, DevicePullRequest req);
    void ack(String deviceId, String requestId, AckRequest req);
}
