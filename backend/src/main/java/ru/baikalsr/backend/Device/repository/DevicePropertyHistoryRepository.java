package ru.baikalsr.backend.Device.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Device.entity.DevicePropertyHistory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DevicePropertyHistoryRepository extends JpaRepository<DevicePropertyHistory, UUID> {

    // серия изменений по одному ключу (новые сверху) + пагинация
    Page<DevicePropertyHistory> findByDevice_IdAndKeyOrderByChangedAtDesc(
            UUID deviceId, String key, Pageable pageable);

    // серия за период (без пагинации, если нужно — перегрузишь Page)
    List<DevicePropertyHistory> findByDevice_IdAndKeyAndChangedAtBetweenOrderByChangedAt(
            UUID deviceId, String key, Instant from, Instant to);

    // последнее значение по ключу
    Optional<DevicePropertyHistory> findFirstByDevice_IdAndKeyOrderByChangedAtDesc(
            UUID deviceId, String key);

    // последнее значение по ключу на момент времени (<= at)
    Optional<DevicePropertyHistory> findFirstByDevice_IdAndKeyAndChangedAtLessThanEqualOrderByChangedAtDesc(
            UUID deviceId, String key, Instant at);

    // housekeeping
    void deleteByDevice_Id(UUID deviceId);
}
