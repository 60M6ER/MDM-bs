package ru.baikalsr.backend.Exchange.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.baikalsr.backend.Exchange.dto.AckRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullResponse;
import ru.baikalsr.backend.Exchange.service.EventSink;
import ru.baikalsr.backend.Exchange.service.ExchangeCache;
import ru.baikalsr.backend.Exchange.service.ExchangeService;
import ru.baikalsr.backend.Exchange.state.StateHandlerRegistry;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeServiceImpl implements ExchangeService {
    private final ExchangeCache cache;
    private final StateHandlerRegistry stateHandlers;
    private final EventSink eventSink;            // куда писать события (кеш/аутбокс/БД)
    private final ObjectMapper om;

    @Override
    public DevicePullResponse pull(String deviceId, String requestId, DevicePullRequest req) {
        if ((requestId != null) && cache.seenRequest(deviceId, requestId)) {
            cache.touchHeartbeat(deviceId, System.currentTimeMillis());
            // опционально: просто вернуть команды/пустой ответ
            var commands = cache.pollCommands(deviceId, 50);
            return new DevicePullResponse(System.currentTimeMillis(), commands);
        }

        var effectiveRequestId = requestId != null ? requestId : UUID.randomUUID().toString();

        // 1) только кладём в кэш, без хендлеров
        cache.storeReport(deviceId, effectiveRequestId, req);
        cache.touchHeartbeat(deviceId, System.currentTimeMillis());

        // 2) отдаём команды как и раньше
        var commands = cache.pollCommands(deviceId, 50);
        return new DevicePullResponse(System.currentTimeMillis(), commands);
    }

    @Override
    public void ack(String deviceId, String requestId, AckRequest req) {
        if (requestId != null && cache.seenRequest(deviceId, requestId)) return;
        cache.storeAcks(deviceId, requestId != null ? requestId : UUID.randomUUID().toString(), req.acks());
        cache.touchHeartbeat(deviceId, System.currentTimeMillis());
    }

    @Scheduled(fixedDelay = 1000)
    public void processReports() {
        while (true) {
            var batch = cache.pollReportsBatch(100); // или по одному
            if (batch.isEmpty()) break;

            for (var report : batch) {
                for (var stateUpdate : report.updates()) {
                    stateHandlerRegistry.dispatch(report.deviceId(), stateUpdate);
                }
                // можно пометить как обработанный, отправить события в EventSink и т.п.
            }
        }
    }
}
