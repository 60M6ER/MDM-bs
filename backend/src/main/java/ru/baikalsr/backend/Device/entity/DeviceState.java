package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Persistable;
import ru.baikalsr.backend.Device.enums.NetworkTypes;

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
@ToString
public class DeviceState implements Persistable<UUID> {

    @Id
    @Column(name = "device_id", nullable = false)
    @EqualsAndHashCode.Include
    private UUID deviceId;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(name = "kiosk_is_on")
    private Boolean kioskIsOn;

    @Column(name = "is_charging")
    private Boolean charging;

    @Column(name = "battery_level")
    private Byte batteryLevel;

    @Column(name = "battery_temperature")
    private Float batteryTemperature;

    @Column(name = "battery_voltage")
    private Float batteryVoltage;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "network_type")
    @Enumerated(EnumType.STRING)
    private NetworkTypes networkType;

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
    @Builder.Default
    private boolean _isNew = false;

    @Override
    public boolean isNew() {
        // можно завязаться на createdUtc == null, но флаг надёжнее
        return _isNew;
    }

    public DeviceState(UUID deviceId) {
        this.deviceId = deviceId;
        this.markNew();
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
}
