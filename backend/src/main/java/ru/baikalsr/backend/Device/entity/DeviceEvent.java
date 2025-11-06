package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import ru.baikalsr.backend.Device.enums.DeviceEvents;

@Entity
@Table(
        name = "device_events",
        indexes = {
                @Index(name = "idx_device_events_device_time", columnList = "device_id, occurred_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEvent {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_events_device")
    )
    private Device device;

    /**
     * Название события — в Java enum, но хранится как строка (varchar).
     * Например: BOOT_COMPLETED, SHUTDOWN, NETWORK_CHANGE, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 200)
    private DeviceEvents event;

    /** Время возникновения события на устройстве или зафиксированное сервером. */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
