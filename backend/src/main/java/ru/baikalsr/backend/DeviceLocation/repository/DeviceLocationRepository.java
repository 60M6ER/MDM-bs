package ru.baikalsr.backend.DeviceLocation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.DeviceLocation.entity.DeviceLocation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceLocationRepository extends JpaRepository<DeviceLocation, UUID> {

    // Все точки устройства за период
    List<DeviceLocation> findByDevice_IdAndTsBetweenOrderByTs(UUID deviceId, Instant from, Instant to);

    // Пагинация точек устройства
    Page<DeviceLocation> findByDevice_IdOrderByTsDesc(UUID deviceId, Pageable pageable);

    // Последняя точка на момент времени
    Optional<DeviceLocation> findFirstByDevice_IdAndTsLessThanEqualOrderByTsDesc(UUID deviceId, Instant at);

    // Удаление всех точек устройства (например, при сбросе)
    void deleteByDevice_Id(UUID deviceId);
}
