package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "device")
public class DeviceState implements Persistable<UUID> {

    // PK = FK → тот же столбец device_id
    @Id
    @Column(name = "device_id", nullable = false)
    @EqualsAndHashCode.Include
    private UUID deviceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // ← говорит, что PK этого entity берётся из FK на Device
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_state_device")
    )
    private Device device;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(name = "is_charging")
    private Boolean charging;

    @Column(name = "battery_level")
    private Short batteryLevel;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "network_type")
    private String networkType; // wifi / mobile / ethernet

    @Column(name = "wifi_ssid")
    private String wifiSsid;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "storage_total_mb")
    private Long storageTotalMb;

    @Column(name = "storage_free_mb")
    private Long storageFreeMb;

    @Column(name = "cpu_temp_c")
    private Double cpuTempC;

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

    @Override
    public UUID getId() {
        return deviceId;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this._isNew = false;
    }

    public void setDevice(Device device) {
        this.device = device;
        if (this.deviceId == null) {
            this.markNew();
        }
        this.deviceId = device.getId();
    }
}
