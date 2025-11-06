package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices",
        indexes = {
                @Index(name = "idx_devices_status", columnList = "status"),
                @Index(name = "idx_devices_serial_number", columnList = "serial_number"),
                @Index(name = "idx_devices_manufacturer_model", columnList = "manufacturer, model")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private UUID id;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column
    private String model;

    @Column
    private String manufacturer;

    @Column(name = "device_name")
    private String deviceName;

    @Column
    private String status;

    @Column(name = "inventory_number")
    private String inventoryNumber;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
