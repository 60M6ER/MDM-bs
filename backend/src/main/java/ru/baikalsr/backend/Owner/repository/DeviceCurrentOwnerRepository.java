package ru.baikalsr.backend.Owner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Owner.entity.DeviceCurrentOwner;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCurrentOwnerRepository extends JpaRepository<DeviceCurrentOwner, UUID> {

    Optional<DeviceCurrentOwner> findByDeviceId(UUID deviceId);

    List<DeviceCurrentOwner> findByDeviceIdIn(Collection<UUID> deviceIds);

    List<DeviceCurrentOwner> findByUserId(UUID userId);
}