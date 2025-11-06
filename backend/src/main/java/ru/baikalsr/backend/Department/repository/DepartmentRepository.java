package ru.baikalsr.backend.Department.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.baikalsr.backend.Department.entity.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    // корневые отделы (parent_id IS NULL)
    List<Department> findByParentIsNullOrderByName();

    // дочерние по родителю
    List<Department> findByParent_IdOrderByName(UUID parentId);

    // поиск по внешнему идентификатору
    Optional<Department> findByExternalId(String externalId);

    // проверка на дубль имени в рамках одного родителя
    boolean existsByNameAndParent_Id(String name, UUID parentId);

    // проверка на дубль корневого имени
    boolean existsByNameAndParentIsNull(String name);
}
