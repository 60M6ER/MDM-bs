package ru.baikalsr.backend.Device.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DeviceState;
import ru.baikalsr.backend.Device.repository.DeviceStateRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceStateService {

    private final DeviceStateRepository deviceStateRepository;

    public List<DeviceState> findAll() {
        return deviceStateRepository.findAll();
    }

    public Optional<DeviceState> findById(UUID id) {
        return deviceStateRepository.findById(id);
    }

    public Optional<DeviceState> findByDeviceId(UUID deviceId) {
        return deviceStateRepository.findByDevice_Id(deviceId);
    }

    @Transactional
    public DeviceState save(DeviceState state) {
        return deviceStateRepository.save(state);
    }

    /**
     * Простая upsert-логика: если у устройства есть запись состояния — обновим ключевые поля,
     * иначе создадим новую. Можно расширить под конкретные поля.
     */
    @Transactional
    public DeviceState upsertForDevice(Device device, java.util.function.Consumer<DeviceState> mutator) {
        var state = deviceStateRepository.findByDevice_Id(device.getId()).orElseGet(() -> DeviceState.builder()
                .device(device)
                .build());
        mutator.accept(state);
        state.setUpdatedAt(Instant.now());
        return deviceStateRepository.save(state);
    }

    @Transactional
    public void delete(UUID id) {
        deviceStateRepository.deleteById(id);
    }
}
