package ru.baikalsr.backend.Department.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "departments",
        indexes = {
                @Index(name = "idx_departments_parent", columnList = "parent_id"),
                @Index(name = "idx_departments_parent_name", columnList = "parent_id, name")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Department {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private UUID id; // генерируется Hibernate

    @Column(nullable = false)
    private String name;

    @Column(name = "external_id")
    private String externalId; // строковый ID из внешней системы (1С)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_departments_parent"))
    private Department parent;

    // удобная навигация; маппинг обратной стороны (не создаёт колонок)
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<Department> children;
}
