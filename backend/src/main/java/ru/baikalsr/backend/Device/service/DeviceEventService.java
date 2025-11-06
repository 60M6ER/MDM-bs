package ru.baikalsr.backend.Device.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DeviceEvent;
import ru.baikalsr.backend.Device.enums.DeviceEvents;
import ru.baikalsr.backend.Device.repository.DeviceEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceEventService {

    private final DeviceEventRepository deviceEventRepository;

    public Page<DeviceEvent> getPage(UUID deviceId, Pageable pageable) {
        return deviceEventRepository.findByDevice_IdOrderByOccurredAtDesc(deviceId, pageable);
    }

    public List<DeviceEvent> getByPeriod(UUID deviceId, Instant from, Instant to) {
        return deviceEventRepository.findByDevice_IdAndOccurredAtBetweenOrderByOccurredAt(deviceId, from, to);
    }

    public List<DeviceEvent> getByTypeAndPeriod(UUID deviceId, DeviceEvents event, Instant from, Instant to) {
        return deviceEventRepository.findByDevice_IdAndEventAndOccurredAtBetweenOrderByOccurredAt(deviceId, event, from, to);
    }

    @Transactional
    public DeviceEvent append(Device device, DeviceEvents event, Instant occurredAt) {
        var e = DeviceEvent.builder()
                .id(UUID.randomUUID())
                .device(device)
                .event(event)
                .occurredAt(occurredAt != null ? occurredAt : Instant.now())
                .build();
        return deviceEventRepository.save(e);
    }

    @Transactional
    public void purgeDevice(UUID deviceId) {
        deviceEventRepository.deleteByDevice_Id(deviceId);
    }
}
