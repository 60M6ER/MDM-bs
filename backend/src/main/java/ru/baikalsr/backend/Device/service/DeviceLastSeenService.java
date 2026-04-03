package ru.baikalsr.backend.Device.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DeviceEvent;
import ru.baikalsr.backend.Device.entity.DeviceState;
import ru.baikalsr.backend.Device.enums.DeviceEvents;
import ru.baikalsr.backend.Device.repository.DeviceEventRepository;
import ru.baikalsr.backend.Device.repository.DeviceRepository;
import ru.baikalsr.backend.Setting.dto.ExchangeSettingsCfg;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.service.SettingsService;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class DeviceLastSeenService {
    private final DeviceRepository deviceRepository;
    private final DeviceStateService deviceStateService;
    private final DeviceEventRepository deviceEventRepository;
    private final SettingsService settingsService;
    private final Executor deviceExchangeExecutor;

    private final ConcurrentMap<UUID, OnlinePresence> onlineDevices = new ConcurrentHashMap<>();

    public DeviceLastSeenService(
            DeviceRepository deviceRepository,
            DeviceStateService deviceStateService,
            DeviceEventRepository deviceEventRepository,
            SettingsService settingsService,
            @Qualifier("applicationTaskExecutor") Executor deviceExchangeExecutor
    ) {
        this.deviceRepository = deviceRepository;
        this.deviceStateService = deviceStateService;
        this.deviceEventRepository = deviceEventRepository;
        this.settingsService = settingsService;
        this.deviceExchangeExecutor = deviceExchangeExecutor;
    }

    @PostConstruct
    void restoreOnlineCache() {
        long nowMs = System.currentTimeMillis();
        long ttlMs = onlineTtlMs();

        int restored = 0;
        int expired = 0;

        for (DeviceState state : deviceStateService.findAllOnlineWithLastSeen()) {
            Instant lastSeenAt = state.getLastSeenAt();
            if (lastSeenAt == null) {
                continue;
            }

            long expiresAtMs = lastSeenAt.toEpochMilli() + ttlMs;
            if (expiresAtMs <= nowMs) {
                expired++;
                UUID deviceId = state.getDeviceId();
                deviceExchangeExecutor.execute(() -> syncOfflineState(deviceId));
                continue;
            }

            onlineDevices.put(
                    state.getDeviceId(),
                    new OnlinePresence(state.getIpAddress(), expiresAtMs)
            );
            restored++;
        }

        log.info("Restored {} online device(s) into last-seen cache, scheduled {} expired device(s) for offline sync", restored, expired);
    }

    public void heartbeat(UUID deviceId, String ipAddress) {
        long nowMs = System.currentTimeMillis();
        long expiresAtMs = nowMs + onlineTtlMs();
        Instant seenAt = Instant.ofEpochMilli(nowMs);

        PresenceChange change = new PresenceChange();
        onlineDevices.compute(deviceId, (id, current) -> {
            String previousIp = current != null ? current.ipAddress() : null;
            change.becameOnline = current == null || current.expiresAtMs() <= nowMs;
            change.ipChanged = ipAddress != null && !ipAddress.equals(previousIp);
            return new OnlinePresence(ipAddress, expiresAtMs);
        });

        deviceExchangeExecutor.execute(() -> syncHeartbeatState(deviceId, ipAddress, seenAt, change.becameOnline, change.ipChanged));
    }

    public boolean isOnline(UUID deviceId) {
        OnlinePresence presence = onlineDevices.get(deviceId);
        return presence != null && presence.expiresAtMs() > System.currentTimeMillis();
    }

    public Map<UUID, Boolean> getOnlineStatuses(Collection<UUID> deviceIds) {
        long nowMs = System.currentTimeMillis();
        Map<UUID, Boolean> result = new LinkedHashMap<>(deviceIds.size());
        for (UUID deviceId : deviceIds) {
            OnlinePresence presence = onlineDevices.get(deviceId);
            result.put(deviceId, presence != null && presence.expiresAtMs() > nowMs);
        }
        return result;
    }

    @Scheduled(fixedDelay = 1000)
    void evictExpiredDevices() {
        long nowMs = System.currentTimeMillis();
        for (var entry : onlineDevices.entrySet()) {
            OnlinePresence presence = entry.getValue();
            if (presence.expiresAtMs() > nowMs) {
                continue;
            }
            if (onlineDevices.remove(entry.getKey(), presence)) {
                UUID deviceId = entry.getKey();
                deviceExchangeExecutor.execute(() -> syncOfflineState(deviceId));
            }
        }
    }

    @Transactional
    void syncHeartbeatState(UUID deviceId, String ipAddress, Instant seenAt, boolean becameOnline, boolean ipChanged) {
        if (!deviceRepository.existsById(deviceId)) {
            log.warn("Device with id {} not found. Skip heartbeat sync from ip: {}", deviceId, ipAddress);
            return;
        }

        Device device = null;
        if (ipChanged) {
            device = deviceRepository.findById(deviceId).orElse(null);
            if (device == null) {
                log.warn("Device with id {} not found while syncing ip change to {}", deviceId, ipAddress);
                return;
            }
        }

        Device finalDevice = device;
        PresenceSyncResult syncResult = new PresenceSyncResult();
        deviceStateService.upsertByDeviceId(deviceId, state -> {
            syncResult.wasOnline = state.isOnline();
            state.setLastSeenAt(seenAt);
            state.setOnline(true);
            syncResult.isOnline = true;
            if (!ipChanged) {
                return;
            }

            String oldIp = state.getIpAddress();
            if (ipAddress == null || ipAddress.equals(oldIp)) {
                return;
            }

            state.setIpAddress(ipAddress);
            DeviceEvent deviceEvent = new DeviceEvent();
            deviceEvent.setDevice(finalDevice);
            deviceEvent.setOccurredAt(seenAt);
            deviceEvent.setEvent(DeviceEvents.IP_CHANGED);
            deviceEventRepository.save(deviceEvent);
            syncResult.ipChanged = true;
            syncResult.oldIp = oldIp;
        });

        if (becameOnline || !syncResult.wasOnline) {
            appendPresenceEvent(deviceId, DeviceEvents.ONLINE, seenAt);
            log.info("Device {} is online, ip={}", deviceId, ipAddress);
        }

        if (syncResult.ipChanged) {
            log.info("Device {} changed ip from {} to {}", deviceId, syncResult.oldIp, ipAddress);
        }
    }

    @Transactional
    void syncOfflineState(UUID deviceId) {
        if (isOnline(deviceId)) {
            return;
        }
        if (!deviceRepository.existsById(deviceId)) {
            return;
        }
        PresenceSyncResult syncResult = new PresenceSyncResult();
        deviceStateService.upsertByDeviceId(deviceId, state -> {
            syncResult.wasOnline = state.isOnline();
            state.setOnline(false);
            syncResult.isOnline = false;
        });

        if (syncResult.wasOnline) {
            appendPresenceEvent(deviceId, DeviceEvents.OFFLINE, Instant.now());
            log.info("Device {} is offline", deviceId);
        }
    }

    private record OnlinePresence(String ipAddress, long expiresAtMs) {
    }

    private static final class PresenceChange {
        private boolean becameOnline;
        private boolean ipChanged;
    }

    private static final class PresenceSyncResult {
        private boolean wasOnline;
        private boolean isOnline;
        private boolean ipChanged;
        private String oldIp;
    }

    private void appendPresenceEvent(UUID deviceId, DeviceEvents event, Instant occurredAt) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            DeviceEvent deviceEvent = new DeviceEvent();
            deviceEvent.setDevice(device);
            deviceEvent.setOccurredAt(occurredAt);
            deviceEvent.setEvent(event);
            deviceEventRepository.save(deviceEvent);
        });
    }

    private long onlineTtlMs() {
        ExchangeSettingsCfg cfg = settingsService.get(SettingGroup.EXCHANGE_SETTINGS, ExchangeSettingsCfg.class);
        long exchangePeriodSec = Math.max(1, cfg.getExchangePeriodSec());
        return Math.round(exchangePeriodSec * 1.5d * 1000d);
    }
}
