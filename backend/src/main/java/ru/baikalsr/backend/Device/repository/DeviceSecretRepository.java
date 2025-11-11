package ru.baikalsr.backend.Device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.baikalsr.backend.Device.entity.DeviceSecret;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSecretRepository extends JpaRepository<DeviceSecret, UUID> {

    Optional<DeviceSecret> findByDeviceId(UUID deviceId);
}
