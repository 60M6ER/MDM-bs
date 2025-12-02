package ru.baikalsr.backend.Exchange.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Exchange.ExchangeCacheProperties;
import ru.baikalsr.backend.Exchange.dto.*;
import ru.baikalsr.backend.Exchange.service.ExchangeCache;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class InMemoryExchangeCache implements ExchangeCache, DisposableBean {
    private final ExchangeCacheProperties props;

    // Очереди команд по устройству
    private final ConcurrentMap<String, Deque<CommandDto>> cmdQueues = new ConcurrentHashMap<>();

    // Идемпотентность: deviceId|requestId -> expireAtMs
    private final ConcurrentMap<String, Long> seenReqUntil = new ConcurrentHashMap<>();

    // Последнее «состояние канала» (для отладки/админки)
    private final ConcurrentMap<String, Long> heartbeats = new ConcurrentHashMap<>();
    // очередь репортов от устройств
    private final Deque<DeviceReport> reportQueue = new ConcurrentLinkedDeque<>();

    // очередь ack'ов от устройств
    private final Deque<DeviceAckBatch> ackQueue = new ConcurrentLinkedDeque<>();

    // Фоновая очистка
    private final ScheduledExecutorService cleaner =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "exchange-cache-cleaner");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    void init() {
        cleaner.scheduleWithFixedDelay(this::cleanup, props.getCleanupIntervalSec(),
                props.getCleanupIntervalSec(), TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        cleaner.shutdownNow();
    }

    @Override
    public void enqueueCommand(String deviceId, CommandDto cmd) {
        Deque<CommandDto> q = cmdQueues.computeIfAbsent(deviceId, k -> new ConcurrentLinkedDeque<>());
        q.addLast(withCreatedAt(cmd));
        // контроль длины очереди (самые старые — первыми под нож)
        while (q.size() > props.getMaxQueueSizePerDevice()) {
            q.pollFirst();
        }
    }

    @Override
    public List<CommandDto> pollCommands(String deviceId, int max) {
        Deque<CommandDto> q = cmdQueues.get(deviceId);
        if (q == null || q.isEmpty()) return List.of();

        List<CommandDto> out = new ArrayList<>(Math.min(max, q.size()));
        for (int i = 0; i < max; i++) {
            CommandDto c = q.pollFirst();
            if (c == null) break;
            if (isExpired(c)) continue; // протухшее не выдаём
            out.add(c);
        }
        return out;
    }

    @Override
    public void storeReport(String deviceId, String requestId, DevicePullRequest req) {
        if (requestId != null) {
            markSeen(deviceId, requestId);
        }

        long now = System.currentTimeMillis();

        reportQueue.addLast(new DeviceReport(
                deviceId,
                requestId,
                req,
                now
        ));

        // ограничиваем размер очереди, чтобы не раздувалась память
        trimReportQueue();
    }

    @Override
    public boolean seenRequest(String deviceId, String requestId) {
        if (requestId == null) return false;
        String key = key(deviceId, requestId);
        Long until = seenReqUntil.get(key);
        long now = System.currentTimeMillis();
        if (until == null) return false;
        if (until < now) {
            // просрочено — уберём лениво
            seenReqUntil.remove(key, until);
            return false;
        }
        return true;
    }

    @Override
    public void storeAcks(String deviceId, String requestId, List<AckDto> acks) {
        if (requestId != null) {
            markSeen(deviceId, requestId);
        }

        if (acks == null || acks.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        ackQueue.addLast(new DeviceAckBatch(
                deviceId,
                requestId,
                List.copyOf(acks),
                now
        ));

        trimAckQueue();
    }

    @Override
    public List<DeviceReport> pollReportsBatch(int batchSize) {
        List<DeviceReport> result = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            DeviceReport r = reportQueue.pollFirst();
            if (r == null) break;
            result.add(r);
        }
        return result;
    }

    @Override
    public List<DeviceAckBatch> pollAcksBatch(int batchSize) {
        List<DeviceAckBatch> result = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            DeviceAckBatch a = ackQueue.pollFirst();
            if (a == null) break;
            result.add(a);
        }
        return result;
    }

    private void trimReportQueue() {
        int max = props.getMaxReportsInQueue(); // новое поле в настройках
        while (reportQueue.size() > max) {
            reportQueue.pollFirst();
        }
    }

    private void trimAckQueue() {
        int max = props.getMaxAcksInQueue(); // тоже из настроек
        while (ackQueue.size() > max) {
            ackQueue.pollFirst();
        }
    }

    @Override
    public void touchHeartbeat(String deviceId, long nowMs) {
        heartbeats.put(deviceId, nowMs);
    }

    // ==== Вспомогательное ====

    private CommandDto withCreatedAt(CommandDto c) {
        if (c.createdAtEpochMs() != null) return c;
        return new CommandDto(c.key(), c.payload(), c.ttlSec(), System.currentTimeMillis());
    }

    private boolean isExpired(CommandDto c) {
        if (c.ttlSec() == null || c.ttlSec() <= 0) return false;
        if (c.createdAtEpochMs() == null) return false;
        long ageSec = (System.currentTimeMillis() - c.createdAtEpochMs()) / 1000;
        return ageSec > c.ttlSec();
    }

    private void markSeen(String deviceId, String requestId) {
        long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(props.getRequestIdTtlSec());
        seenReqUntil.put(key(deviceId, requestId), until);
    }

    private String key(String deviceId, String requestId) {
        return deviceId + '|' + requestId;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();

        // очищаем seenReqUntil
        for (var e : seenReqUntil.entrySet()) {
            if (e.getValue() < now) seenReqUntil.remove(e.getKey(), e.getValue());
        }
        cleanupReportsAndAcks();

        // чистим heartbeat, если очень старые (для экономии памяти)
        long hbCut = now - TimeUnit.SECONDS.toMillis(props.getHeartbeatTtlSec());
        for (var e : heartbeats.entrySet()) {
            if (e.getValue() < hbCut) heartbeats.remove(e.getKey(), e.getValue());
        }

        // срезаем протухшие команды в головах очередей
        for (var entry : cmdQueues.entrySet()) {
            Deque<CommandDto> q = entry.getValue();
            if (q == null || q.isEmpty()) continue;
            // удаляем только ведущие протухшие элементы, чтобы не перебирать всю очередь
            while (true) {
                CommandDto head = q.peekFirst();
                if (head == null) break;
                if (!isExpired(head)) break;
                q.pollFirst();
            }
        }
    }

    private void cleanupReportsAndAcks() {
        long now = System.currentTimeMillis();
        long reportCut = now - TimeUnit.SECONDS.toMillis(props.getReportTtlSec());
        long ackCut = now - TimeUnit.SECONDS.toMillis(props.getAckTtlSec());

        // чистим голову очереди, пока элементы старше TTL
        while (true) {
            DeviceReport head = reportQueue.peekFirst();
            if (head == null || head.receivedAtMs() >= reportCut) break;
            reportQueue.pollFirst();
        }

        while (true) {
            DeviceAckBatch head = ackQueue.peekFirst();
            if (head == null || head.receivedAtMs() >= ackCut) break;
            ackQueue.pollFirst();
        }
    }
}
