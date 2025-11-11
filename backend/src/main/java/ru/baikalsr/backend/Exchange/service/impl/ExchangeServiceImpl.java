package ru.baikalsr.backend.Exchange.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
            //cache.touchHeartbeat(deviceId, System.currentTimeMillis());
            return new DevicePullResponse(System.currentTimeMillis(), cache.pollCommands(deviceId, 50));
        }

        // состояния — строго через хендлеры по ключам
        for (var s : req.states()) stateHandlers.dispatch(deviceId, s);

        // события — можно сразу в sink (батчем)
        eventSink.append(deviceId, req.events());

        cache.storeReport(deviceId, requestId != null ? requestId : UUID.randomUUID().toString(), req);
        cache.touchHeartbeat(deviceId, System.currentTimeMillis());

        var commands = cache.pollCommands(deviceId, 50);
        return new DevicePullResponse(System.currentTimeMillis(), commands);
    }

    @Override
    public void ack(String deviceId, String requestId, AckRequest req) {
        if (requestId != null && cache.seenRequest(deviceId, requestId)) return;
        cache.storeAcks(deviceId, requestId != null ? requestId : UUID.randomUUID().toString(), req.acks());
        cache.touchHeartbeat(deviceId, System.currentTimeMillis());
    }
}
