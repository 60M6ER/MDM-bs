package ru.baikalsr.backend.Owner.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Owner.entity.DeviceOwner;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceOwnerRepository extends JpaRepository<DeviceOwner, Long> {

    // История владельцев устройства (свежие сверху) + пагинация
    Page<DeviceOwner> findByDevice_IdOrderByAssignedAtDesc(UUID deviceId, Pageable pageable);

    // История за период
    List<DeviceOwner> findByDevice_IdAndAssignedAtBetweenOrderByAssignedAt(
            UUID deviceId, Instant from, Instant to);

    // Текущий владелец (последняя запись)
    Optional<DeviceOwner> findFirstByDevice_IdOrderByAssignedAtDesc(UUID deviceId);

    // По пользователю
    Page<DeviceOwner> findByUserIdOrderByAssignedAtDesc(UUID userId, Pageable pageable);
}