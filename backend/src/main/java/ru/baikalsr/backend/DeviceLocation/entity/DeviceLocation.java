package ru.baikalsr.backend.DeviceLocation.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.baikalsr.backend.Device.entity.Device;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "device_locations",
        indexes = {
                @Index(name = "idx_dl_device_ts", columnList = "device_id, ts")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceLocation {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_locations_device")
    )
    private Device device;

    /** Время, зафиксированное на устройстве или сенсоре */
    @Column(name = "ts", nullable = false)
    private Instant ts;

    /** Время, когда запись получена сервером */
    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lon", nullable = false)
    private Double lon;

    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "altitude_m")
    private Double altitudeM;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "heading_deg")
    private Double headingDeg;

    /** gps / network / fused / manual / unknown */
    @Column(name = "source")
    private String source;

    /** Признак поддельных координат */
    @Column(name = "is_mock")
    private Boolean mock;
}
