package ru.baikalsr.backend.Department.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Department.entity.DeviceDepartment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceDepartmentRepository extends JpaRepository<DeviceDepartment, UUID> {

    // История закреплений устройства (новые сверху)
    Page<DeviceDepartment> findByDevice_IdOrderByAssignedAtDesc(UUID deviceId, Pageable pageable);

    // История за период
    List<DeviceDepartment> findByDevice_IdAndAssignedAtBetweenOrderByAssignedAt(
            UUID deviceId, Instant from, Instant to);

    // Последняя запись на момент времени
    Optional<DeviceDepartment> findFirstByDevice_IdAndAssignedAtLessThanEqualOrderByAssignedAtDesc(
            UUID deviceId, Instant at);

    // Все устройства, прикреплённые к конкретному подразделению
    List<DeviceDepartment> findByDepartment_IdOrderByAssignedAtDesc(UUID departmentId);

    // Очистка истории устройства
    void deleteByDevice_Id(UUID deviceId);
}