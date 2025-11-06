package ru.baikalsr.backend.Owner.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.baikalsr.backend.Device.entity.Device;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "device_owners",
        indexes = {
                @Index(name = "idx_device_owners_device_date", columnList = "device_id, assigned_at"),
                @Index(name = "idx_device_owners_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // bigserial

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_owners_device")
    )
    private Device device;

    /** Строковое представление владельца (логин/имя/email). Может быть null, если задан userId. */
    @Column(name = "owner_display")
    private String ownerDisplay;

    /** Опциональная ссылка на пользователя (FK в БД), маппим как UUID чтобы не зависеть от User-entity. */
    @Column(name = "user_id")
    private UUID userId;

    /** Дата назначения владельца (NOT NULL, default now() в БД) */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
