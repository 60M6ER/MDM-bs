package ru.baikalsr.backend.Device.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_secrets")
@Data
public class DeviceSecret implements Persistable<UUID> {

    @Id
    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "created_utc")
    private Instant createdUtc;

    @Transient
    private boolean _isNew = false;

    @Override
    public UUID getId() {
        return deviceId;
    }

    @Override
    public boolean isNew() {
        // можно завязаться на createdUtc == null, но флаг надёжнее
        return _isNew;
    }

    /** Вызывай это перед сохранением нового устройства */
    public void markNew() {
        this._isNew = true;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this._isNew = false;
    }
}
