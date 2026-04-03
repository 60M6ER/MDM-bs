package ru.baikalsr.backend.Exchange.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.baikalsr.backend.Device.service.DeviceLastSeenService;
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
    private final DeviceLastSeenService deviceLastSeenService;
    private final EventSink eventSink;            // куда писать события (кеш/аутбокс/БД)

    @Override
    public DevicePullResponse pull(String deviceId, String requestId, DevicePullRequest req, HttpServletRequest request) {
        if ((requestId != null) && cache.seenRequest(deviceId, requestId)) {
            deviceLastSeenService.heartbeat(UUID.fromString(deviceId), request.getRemoteAddr());
            // опционально: просто вернуть команды/пустой ответ
            var commands = cache.pollCommands(deviceId, 50);
            log.info(
                    "Device {} duplicate pull, requestId={}, ip={}, states={}, events={}, commands={}",
                    deviceId,
                    requestId,
                    request.getRemoteAddr(),
                    safeSize(req.states()),
                    safeSize(req.events()),
                    commands.size()
            );
            return new DevicePullResponse(System.currentTimeMillis(), commands);
        }

        var effectiveRequestId = requestId != null ? requestId : UUID.randomUUID().toString();

        deviceLastSeenService.heartbeat(UUID.fromString(deviceId), request.getRemoteAddr());

        // 1) только кладём в кэш, без хендлеров
        cache.storeReport(deviceId, effectiveRequestId, req);

        // 2) отдаём команды как и раньше
        var commands = cache.pollCommands(deviceId, 50);
        log.info(
                "Device {} pull accepted, requestId={}, ip={}, states={}, events={}, commands={}",
                deviceId,
                effectiveRequestId,
                request.getRemoteAddr(),
                safeSize(req.states()),
                safeSize(req.events()),
                commands.size()
        );
        return new DevicePullResponse(System.currentTimeMillis(), commands);
    }

    @Override
    public void ack(String deviceId, String requestId, AckRequest req, HttpServletRequest request) {
        deviceLastSeenService.heartbeat(UUID.fromString(deviceId), request.getRemoteAddr());
        if (requestId != null && cache.seenRequest(deviceId, requestId)) {
            log.info(
                    "Device {} duplicate ack, requestId={}, ip={}, acks={}",
                    deviceId,
                    requestId,
                    request.getRemoteAddr(),
                    safeSize(req.acks())
            );
            return;
        }
        String effectiveRequestId = requestId != null ? requestId : UUID.randomUUID().toString();
        cache.storeAcks(deviceId, effectiveRequestId, req.acks());
        log.info(
                "Device {} ack accepted, requestId={}, ip={}, acks={}",
                deviceId,
                effectiveRequestId,
                request.getRemoteAddr(),
                safeSize(req.acks())
        );
    }

    @Override
    public void sendCommand(String deviceId, CommandDto command) {
        cache.enqueueCommand(deviceId, command);
        log.info(
                "Enqueued command {} for device {}, ttlSec={}",
                command.key(),
                deviceId,
                command.ttlSec()
        );
    }

    @Scheduled(fixedDelay = 1000)
    public void processReports() {
        while (true) {
            List<DeviceReport> batch = cache.pollReportsBatch(100);
            if (batch.isEmpty()) {
                break;
            }

            log.info("Processing device report batch, size={}", batch.size());

            for (DeviceReport report : batch) {
                DevicePullRequest payload = report.payload();
                if (payload == null) {
                    log.warn(
                            "Skip empty device report, deviceId={}, requestId={}, receivedAt={}",
                            report.deviceId(),
                            report.requestId(),
                            report.receivedAtMs()
                    );
                    continue;
                }

                log.info(
                        "Processing device report, deviceId={}, requestId={}, states={}, events={}, receivedAt={}",
                        report.deviceId(),
                        report.requestId(),
                        safeSize(payload.states()),
                        safeSize(payload.events()),
                        report.receivedAtMs()
                );

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

    private int safeSize(List<?> items) {
        return items != null ? items.size() : 0;
    }
}
