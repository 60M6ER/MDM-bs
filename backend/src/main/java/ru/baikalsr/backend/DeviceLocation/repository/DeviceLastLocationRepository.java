package ru.baikalsr.backend.DeviceLocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.DeviceLocation.entity.DeviceLastLocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceLastLocationRepository extends JpaRepository<DeviceLastLocation, UUID> {

    Optional<DeviceLastLocation> findByDeviceId(UUID deviceId);

    List<DeviceLastLocation> findByDeviceIdIn(Collection<UUID> deviceIds);
}
