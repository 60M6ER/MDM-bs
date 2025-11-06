package ru.baikalsr.backend.Device.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DevicePropertyHistory;
import ru.baikalsr.backend.Device.repository.DevicePropertyHistoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevicePropertyHistoryService {

    private final DevicePropertyHistoryRepository devicePropertyHistoryRepository;

    public Page<DevicePropertyHistory> getSeries(UUID deviceId, String key, Pageable pageable) {
        return devicePropertyHistoryRepository.findByDevice_IdAndKeyOrderByChangedAtDesc(deviceId, key, pageable);
    }

    public List<DevicePropertyHistory> getSeries(UUID deviceId, String key, Instant from, Instant to) {
        return devicePropertyHistoryRepository.findByDevice_IdAndKeyAndChangedAtBetweenOrderByChangedAt(deviceId, key, from, to);
    }

    public Optional<DevicePropertyHistory> getLastAt(UUID deviceId, String key, Instant at) {
        return devicePropertyHistoryRepository.findFirstByDevice_IdAndKeyAndChangedAtLessThanEqualOrderByChangedAtDesc(deviceId, key, at);
    }

    @Transactional
    public DevicePropertyHistory append(Device device, String key, JsonNode value, String source, Instant changedAt) {
        var h = DevicePropertyHistory.builder()
                .id(UUID.randomUUID())
                .device(device)
                .key(key)
                .value(value)
                .source(source)
                .changedAt(changedAt != null ? changedAt : Instant.now())
                .build();
        return devicePropertyHistoryRepository.save(h);
    }

    @Transactional
    public void purgeDevice(UUID deviceId) {
        devicePropertyHistoryRepository.deleteByDevice_Id(deviceId);
    }
}