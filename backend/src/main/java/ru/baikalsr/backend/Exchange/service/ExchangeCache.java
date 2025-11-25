package ru.baikalsr.backend.Exchange.service;

import ru.baikalsr.backend.Exchange.dto.AckDto;
import ru.baikalsr.backend.Exchange.dto.CommandDto;
import ru.baikalsr.backend.Exchange.dto.DevicePullRequest;

import java.util.List;

public interface ExchangeCache {
    void enqueueCommand(String deviceId, CommandDto cmd);
    List<CommandDto> pollCommands(String deviceId, int max);              // выдаёт пакет команд, отфильтровав протухшие
    void storeReport(String deviceId, String requestId, DevicePullRequest req);
    boolean seenRequest(String deviceId, String requestId);               // идемпотентность по X-Request-Id
    void storeAcks(String deviceId, String requestId, List<AckDto> acks);
    void touchHeartbeat(String deviceId, long nowMs);
    pollReportsBatch(int batchSize);
}
