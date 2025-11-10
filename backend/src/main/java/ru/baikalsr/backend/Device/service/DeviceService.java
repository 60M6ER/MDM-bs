package ru.baikalsr.backend.Device.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.dto.DeviceListItemDto;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DeviceDetailsCurrent;
import ru.baikalsr.backend.Device.mapper.DeviceDetailsMapper;
import ru.baikalsr.backend.Device.repository.DeviceDetailsCurrentRepository;
import ru.baikalsr.backend.Device.repository.DeviceRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceDetailsCurrentRepository deviceDetailsCurrentRepository;
    private final DeviceDetailsMapper deviceDetailsMapper;

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Page<DeviceListItemDto> findAll(Pageable pageable) {
        return deviceDetailsMapper.toListItems(deviceRepository.findAll(pageable));
    }

    public Optional<Device> findById(UUID id) {
        return deviceRepository.findById(id);
    }

    public Optional<Device>  findBySerialNumber(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber);
    }

    /** Получить детальную карточку устройства */
    public Optional<DeviceDetailsCurrent> get(UUID deviceId) {
        return deviceDetailsCurrentRepository.findByDeviceId(deviceId);
    }

    @Transactional
    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    @Transactional
    public void delete(UUID id) {
        deviceRepository.deleteById(id);
    }

    public boolean existsBySerialNumber(String serialNumber) {
        return deviceRepository.existsBySerialNumber(serialNumber);
    }
}
