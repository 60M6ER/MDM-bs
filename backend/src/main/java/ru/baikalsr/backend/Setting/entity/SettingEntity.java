package ru.baikalsr.backend.Setting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingEntity {

    @Id
    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String value;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
