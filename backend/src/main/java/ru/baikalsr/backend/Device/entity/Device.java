package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;
import ru.baikalsr.backend.Device.enums.DeviceStatus;

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
public class Device implements Persistable<UUID> {

    @Id
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DeviceStatus status;

    @Column(name = "inventory_number")
    private String inventoryNumber;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id", referencedColumnName = "device_id")
    private DeviceSecret deviceSecret;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    private boolean _isNew = false;

    @Override
    public boolean isNew() {
        // можно завязаться на createdUtc == null, но флаг надёжнее
        return _isNew;
    }

    /** Вызывай это перед сохранением нового устройства */
    public void markNew() {
        this._isNew = true;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this._isNew = false;
    }
}
