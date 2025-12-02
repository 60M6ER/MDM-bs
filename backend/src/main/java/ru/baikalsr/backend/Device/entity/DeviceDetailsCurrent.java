package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_details_current")
@Immutable
@Getter
@NoArgsConstructor
public class DeviceDetailsCurrent {

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "inventory_number")
    private String inventoryNumber;

    @Column(name = "status")
    private String status;

    @Column(name = "model")
    private String model;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "device_created_at")
    private Instant deviceCreatedAt;

    @Column(name = "device_updated_at")
    private Instant deviceUpdatedAt;

    // --- State ---
    @Column(name = "state_last_seen_at")
    private Instant stateLastSeenAt;

    @Column(name = "state_updated_at")
    private Instant stateUpdatedAt;

    @Column(name = "is_online")
    private Boolean online;

    @Column(name = "kiosk_is_on")
    private Boolean kioskIsOn;

    @Column(name = "is_charging")
    private Boolean charging;

    @Column(name = "battery_level")
    private Short batteryLevel;

    @Column(name = "battery_temperature")
    private Double batteryTemperature;

    @Column(name = "battery_voltage")
    private Double batteryVoltage;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "network_type")
    private String networkType;

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

    // --- Owner ---
    @Column(name = "owner_display")
    private String ownerDisplay;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "owner_assigned_at")
    private Instant ownerAssignedAt;

    // --- Location ---
    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "altitude_m")
    private Double altitudeM;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "heading_deg")
    private Double headingDeg;

    @Column(name = "location_source")
    private String locationSource;

    @Column(name = "location_is_mock")
    private Boolean locationIsMock;

    @Column(name = "location_ts")
    private Instant locationTs;

    @Column(name = "location_received_at")
    private Instant locationReceivedAt;

    // --- Department ---
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "department_assigned_at")
    private Instant departmentAssignedAt;
}
