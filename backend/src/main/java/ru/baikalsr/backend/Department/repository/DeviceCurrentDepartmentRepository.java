package ru.baikalsr.backend.Department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Department.entity.DeviceCurrentDepartment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCurrentDepartmentRepository extends JpaRepository<DeviceCurrentDepartment, UUID> {

    Optional<DeviceCurrentDepartment> findByDeviceId(UUID deviceId);

    List<DeviceCurrentDepartment> findByDeviceIdIn(Collection<UUID> deviceIds);

    List<DeviceCurrentDepartment> findByDepartmentId(UUID departmentId);
}
