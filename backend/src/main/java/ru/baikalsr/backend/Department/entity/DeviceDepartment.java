package ru.baikalsr.backend.Department.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.baikalsr.backend.Device.entity.Device;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "device_departments",
        indexes = {
                @Index(name = "idx_dd_device_time_desc", columnList = "device_id, assigned_at DESC"),
                @Index(name = "idx_dd_department_time", columnList = "department_id, assigned_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dd_device_time_dept", columnNames = {"device_id", "assigned_at", "department_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceDepartment {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_departments_device")
    )
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "department_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_departments_department")
    )
    private Department department;

    /** Дата назначения подразделения устройству */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
