package ru.baikalsr.backend.Owner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Owner.entity.DeviceCurrentOwner;
import ru.baikalsr.backend.Owner.entity.DeviceOwner;
import ru.baikalsr.backend.Owner.repository.DeviceCurrentOwnerRepository;
import ru.baikalsr.backend.Owner.repository.DeviceOwnerRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceOwnerService {

    private final DeviceOwnerRepository deviceOwnerRepository;
    private final DeviceCurrentOwnerRepository deviceCurrentOwnerRepository;

    public Page<DeviceOwner> history(UUID deviceId, Pageable pageable) {
        return deviceOwnerRepository.findByDevice_IdOrderByAssignedAtDesc(deviceId, pageable);
    }

    public List<DeviceOwner> history(UUID deviceId, Instant from, Instant to) {
        return deviceOwnerRepository.findByDevice_IdAndAssignedAtBetweenOrderByAssignedAt(deviceId, from, to);
    }

    public Optional<DeviceOwner> currentOwner(UUID deviceId) {
        return deviceOwnerRepository.findFirstByDevice_IdOrderByAssignedAtDesc(deviceId);
    }

    public Page<DeviceOwner> byUser(UUID userId, Pageable pageable) {
        return deviceOwnerRepository.findByUserIdOrderByAssignedAtDesc(userId, pageable);
    }

    /** Текущий владелец устройства (вью device_current_owner) */
    public Optional<DeviceCurrentOwner> current(UUID deviceId) {
        return deviceCurrentOwnerRepository.findByDeviceId(deviceId);
    }

    /** Текущие владельцы для набора устройств */
    public List<DeviceCurrentOwner> currentForDevices(Collection<UUID> deviceIds) {
        return deviceCurrentOwnerRepository.findByDeviceIdIn(deviceIds);
    }

    /** Список устройств, где текущий владелец — указанный пользователь */
    public List<DeviceCurrentOwner> currentByUser(UUID userId) {
        return deviceCurrentOwnerRepository.findByUserId(userId);
    }

    @Transactional
    public DeviceOwner append(Device device, String ownerDisplay, UUID userId, Instant assignedAt) {
        var rec = DeviceOwner.builder()
                .device(device)
                .ownerDisplay(ownerDisplay)
                .userId(userId)
                .assignedAt(assignedAt != null ? assignedAt : Instant.now())
                .build();
        return deviceOwnerRepository.save(rec);
    }

    @Transactional
    public void delete(Long id) {
        deviceOwnerRepository.deleteById(id);
    }
}
