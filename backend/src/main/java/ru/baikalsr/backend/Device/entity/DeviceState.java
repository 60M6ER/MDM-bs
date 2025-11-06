package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceState {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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
}
