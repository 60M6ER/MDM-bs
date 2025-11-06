package ru.baikalsr.backend.Device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.baikalsr.backend.Device.entity.DeviceDetailsCurrent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceDetailsCurrentRepository extends JpaRepository<DeviceDetailsCurrent, UUID> {

    /** Получить данные по одному устройству */
    Optional<DeviceDetailsCurrent> findByDeviceId(UUID deviceId);

    /** Получить все устройства, у которых статус совпадает */
    List<DeviceDetailsCurrent> findByStatus(String status);

    /** Поиск устройств по владельцу (через userId из текущего владельца) */
    List<DeviceDetailsCurrent> findByOwnerUserId(UUID ownerUserId);

    /** Поиск устройств по департаменту */
    List<DeviceDetailsCurrent> findByDepartmentId(UUID departmentId);
}