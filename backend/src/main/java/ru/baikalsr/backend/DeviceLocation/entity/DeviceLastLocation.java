package ru.baikalsr.backend.DeviceLocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_last_location")
@Immutable                      // read-only: Hibernate не будет делать UPDATE/INSERT/DELETE
@Getter
@NoArgsConstructor              // JPA нужен пустой конструктор
public class DeviceLastLocation {

    @Id                         // во вью одна строка на device_id → PK можно ставить на него
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

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

    @Column(name = "source")
    private String source;

    @Column(name = "is_mock")
    private Boolean mock;
}
