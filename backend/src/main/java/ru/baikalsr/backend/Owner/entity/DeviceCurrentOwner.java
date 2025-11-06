package ru.baikalsr.backend.Owner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import ru.baikalsr.backend.Device.entity.Device;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_current_owner")
@Immutable                   // вью, только чтение
@Getter
@NoArgsConstructor
public class DeviceCurrentOwner {

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "owner_display")
    private String ownerDisplay;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    // Необязательная навигация на устройство (read-only, без вставок/апдейтов)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", insertable = false, updatable = false)
    private Device device;
}