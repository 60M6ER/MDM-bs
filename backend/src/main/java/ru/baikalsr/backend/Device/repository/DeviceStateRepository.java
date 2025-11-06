package ru.baikalsr.backend.Device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.baikalsr.backend.Device.entity.DeviceState;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceStateRepository extends JpaRepository<DeviceState, UUID> {

    // текущее состояние по ID устройства
    Optional<DeviceState> findByDevice_Id(UUID deviceId);

    boolean existsByDevice_Id(UUID deviceId);
}
