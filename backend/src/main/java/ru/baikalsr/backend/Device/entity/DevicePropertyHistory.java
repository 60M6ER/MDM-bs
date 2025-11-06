package ru.baikalsr.backend.Device.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "device_property_history",
        indexes = {
                @Index(name = "idx_dph_device_key_time", columnList = "device_id, key, changed_at"),
                // этот ниже отражает GIN в БД только документарно; JPA его реально не создаст
                // @Index(name = "idx_dph_value_gin", columnList = "value")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevicePropertyHistory {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dph_device")
    )
    private Device device;

    @Column(name = "key", length = 200, nullable = false)
    private String key;

    @Type(JsonType.class)                           // hibernate-types-60
    @Column(name = "value", columnDefinition = "jsonb", nullable = false)
    private JsonNode value;                         // можно заменить на Map<String, Object>

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "source")
    private String source;                          // "device" / "server" / "agent"
}