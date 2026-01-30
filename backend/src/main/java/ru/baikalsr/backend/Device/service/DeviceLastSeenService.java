package ru.baikalsr.backend.Device.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DeviceEvent;
import ru.baikalsr.backend.Device.entity.DeviceState;
import ru.baikalsr.backend.Device.enums.DeviceEvents;
import ru.baikalsr.backend.Device.repository.DeviceEventRepository;
import ru.baikalsr.backend.Device.repository.DeviceRepository;
import ru.baikalsr.backend.Device.repository.DeviceStateRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceLastSeenService {
    private final DeviceRepository deviceRepository;
    private final DeviceStateRepository deviceStateRepository;
    private final DeviceEventRepository deviceEventRepository;

    /**
     * Запускается асинхронно: контроллер может сразу вернуть ответ клиенту.
     */
    @Async("deviceExchangeExecutor")
    @Transactional
    public void updateLastSeenIpAsync(UUID deviceId, HttpServletRequest request) {
        String newIp = request.getRemoteAddr(); // пока без forwarded

        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            log.warn("Device with id {} not found. Last seen service from ip: {}", deviceId, newIp);
            return;
        }

        DeviceState state = deviceStateRepository.findByDevice_Id(deviceId).orElse(null);
        if (state == null) {
            state = new DeviceState(deviceId);
        }

        //state.setIpAddress(newIp);

        String oldIp = state.getIpAddress();
        if (newIp != null && !newIp.equals(oldIp)) {
            state.setIpAddress(newIp);

            DeviceEvent deviceEvent = new DeviceEvent();
            deviceEvent.setDevice(device);
            deviceEvent.setOccurredAt(Instant.now());
            deviceEvent.setEvent(DeviceEvents.IP_CHANGED);

            deviceEventRepository.save(deviceEvent);
        }
        deviceStateRepository.save(state);
    }
}
