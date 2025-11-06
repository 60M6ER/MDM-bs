package ru.baikalsr.backend.Department.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_current_department")
@Immutable                 // вью, только чтение
@Getter
@NoArgsConstructor
public class DeviceCurrentDepartment {

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    // Необязательные навигационные связи (read-only), если нужно работать с объектами:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", insertable = false, updatable = false)
    private ru.baikalsr.backend.Device.entity.Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;
}
