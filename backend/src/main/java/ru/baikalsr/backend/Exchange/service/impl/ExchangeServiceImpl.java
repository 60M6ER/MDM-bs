package ru.baikalsr.backend.Exchange.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.baikalsr.backend.Exchange.dto.*;
import ru.baikalsr.backend.Exchange.service.EventSink;
import ru.baikalsr.backend.Exchange.service.ExchangeCache;
import ru.baikalsr.backend.Exchange.service.ExchangeService;
import ru.baikalsr.backend.Exchange.service.event.EventHandlerRegistry;
import ru.baikalsr.backend.Exchange.state.StateHandlerRegistry;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeServiceImpl implements ExchangeService {
    private final ExchangeCache cache;
    private final StateHandlerRegistry stateHandlers;
    private final EventHandlerRegistry eventHandlers;
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

    @Override
    public void sendCommand(String deviceId, CommandDto command) {
        cache.enqueueCommand(deviceId, command);
    }

    @Scheduled(fixedDelay = 1000)
    public void processReports() {
        while (true) {
            List<DeviceReport> batch = cache.pollReportsBatch(100);
            if (batch.isEmpty()) {
                break;
            }

            for (DeviceReport report : batch) {
                DevicePullRequest payload = report.payload();
                if (payload == null) {
                    continue;
                }

                // 1. Обработка состояний
                if (payload.states() != null && !payload.states().isEmpty()) {
                    for (StateUpdate stateUpdate : payload.states()) {
                        try {
                            stateHandlers.dispatch(report.deviceId(), stateUpdate);
                        } catch (Exception e) {
                            log.error(
                                    "Failed to process state {} from device {} (requestId={}, receivedAt={})",
                                    stateUpdate.key(),
                                    report.deviceId(),
                                    report.requestId(),
                                    report.receivedAtMs(),
                                    e
                            );
                            // сюда можно добавить DLQ/метрики
                        }
                    }
                }

                // 2. Обработка событий
                if (payload.events() != null && !payload.events().isEmpty()) {
                    for (EventItem eventItem : payload.events()) {
                        try {
                            // если нужно писать «сырые» события в EventSink, можно сделать:
                            // eventSink.accept(report.deviceId(), eventItem);
                            eventHandlers.dispatch(report.deviceId(), eventItem);
                        } catch (Exception e) {
                            log.error(
                                    "Failed to process event {} from device {} (requestId={}, receivedAt={})",
                                    eventItem.key(),
                                    report.deviceId(),
                                    report.requestId(),
                                    report.receivedAtMs(),
                                    e
                            );
                            // DLQ/метрики по событиям
                        }
                    }
                }
            }
        }
    }
}
