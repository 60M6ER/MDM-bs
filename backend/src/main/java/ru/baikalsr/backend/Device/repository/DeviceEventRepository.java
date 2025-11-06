package ru.baikalsr.backend.Device.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Device.entity.DeviceEvent;
import ru.baikalsr.backend.Device.enums.DeviceEvents;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {

    // История событий по устройству (новые сверху)
    Page<DeviceEvent> findByDevice_IdOrderByOccurredAtDesc(UUID deviceId, Pageable pageable);

    // Фильтр по диапазону времени
    List<DeviceEvent> findByDevice_IdAndOccurredAtBetweenOrderByOccurredAt(
            UUID deviceId, Instant from, Instant to);

    // Фильтр по типу события и периоду
    List<DeviceEvent> findByDevice_IdAndEventAndOccurredAtBetweenOrderByOccurredAt(
            UUID deviceId, DeviceEvents event, Instant from, Instant to);

    // Массовое удаление событий устройства
    void deleteByDevice_Id(UUID deviceId);
}
