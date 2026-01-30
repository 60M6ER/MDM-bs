package ru.baikalsr.backend.Exchange.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.baikalsr.backend.Exchange.dto.AckRequest;
import ru.baikalsr.backend.Exchange.dto.CommandDto;
import ru.baikalsr.backend.Exchange.dto.DevicePullRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullResponse;

public interface ExchangeService {
    DevicePullResponse pull(String deviceId, String requestId, DevicePullRequest req, HttpServletRequest request);
    void ack(String deviceId, String requestId, AckRequest req, HttpServletRequest request);
    void sendCommand(String deviceId, CommandDto command);
}
