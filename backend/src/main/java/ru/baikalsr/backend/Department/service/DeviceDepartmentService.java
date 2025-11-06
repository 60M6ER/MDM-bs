package ru.baikalsr.backend.Department.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Department.entity.Department;
import ru.baikalsr.backend.Department.entity.DeviceCurrentDepartment;
import ru.baikalsr.backend.Department.entity.DeviceDepartment;
import ru.baikalsr.backend.Department.repository.DeviceCurrentDepartmentRepository;
import ru.baikalsr.backend.Department.repository.DeviceDepartmentRepository;
import ru.baikalsr.backend.Device.entity.Device;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceDepartmentService {

    private final DeviceDepartmentRepository deviceDepartmentRepository;
    private final DeviceCurrentDepartmentRepository currentRepo;

    public Page<DeviceDepartment> history(UUID deviceId, Pageable pageable) {
        return deviceDepartmentRepository.findByDevice_IdOrderByAssignedAtDesc(deviceId, pageable);
    }

    public List<DeviceDepartment> history(UUID deviceId, Instant from, Instant to) {
        return deviceDepartmentRepository.findByDevice_IdAndAssignedAtBetweenOrderByAssignedAt(deviceId, from, to);
    }

    public Optional<DeviceDepartment> at(UUID deviceId, Instant at) {
        return deviceDepartmentRepository.findFirstByDevice_IdAndAssignedAtLessThanEqualOrderByAssignedAtDesc(deviceId, at);
    }

    public List<DeviceDepartment> byDepartment(UUID departmentId) {
        return deviceDepartmentRepository.findByDepartment_IdOrderByAssignedAtDesc(departmentId);
    }

    /** Текущий департамент конкретного устройства (по вью device_current_department) */
    public Optional<DeviceCurrentDepartment> current(UUID deviceId) {
        return currentRepo.findByDeviceId(deviceId);
    }

    /** Текущие департаменты для набора устройств */
    public List<DeviceCurrentDepartment> currentForDevices(Collection<UUID> deviceIds) {
        return currentRepo.findByDeviceIdIn(deviceIds);
    }

    /** Список устройств, которые сейчас находятся в указанном департаменте */
    public List<DeviceCurrentDepartment> devicesCurrentlyInDepartment(UUID departmentId) {
        return currentRepo.findByDepartmentId(departmentId);
    }

    @Transactional
    public DeviceDepartment append(Device device, Department department, Instant assignedAt) {
        var link = DeviceDepartment.builder()
                .id(UUID.randomUUID())
                .device(device)
                .department(department)
                .assignedAt(assignedAt != null ? assignedAt : Instant.now())
                .build();
        return deviceDepartmentRepository.save(link);
    }

    @Transactional
    public void purgeDevice(UUID deviceId) {
        deviceDepartmentRepository.deleteByDevice_Id(deviceId);
    }
}