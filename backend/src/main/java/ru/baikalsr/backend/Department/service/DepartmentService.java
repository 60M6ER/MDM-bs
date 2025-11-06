package ru.baikalsr.backend.Department.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Department.entity.Department;
import ru.baikalsr.backend.Department.repository.DepartmentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository repo;

    public List<Department> roots() {
        return repo.findByParentIsNullOrderByName();
    }

    public List<Department> children(UUID parentId) {
        return repo.findByParent_IdOrderByName(parentId);
    }

    public Optional<Department> byId(UUID id) {
        return repo.findById(id);
    }

    public Optional<Department> byExternalId(String externalId) {
        return repo.findByExternalId(externalId);
    }

    @Transactional
    public Department create(String name, String externalId, Department parent) {
        var d = Department.builder()
                .id(UUID.randomUUID())
                .name(name)
                .externalId(externalId)
                .parent(parent)
                .build();
        return repo.save(d);
    }

    @Transactional
    public Department rename(UUID id, String newName) {
        var d = repo.findById(id).orElseThrow();
        d.setName(newName);
        return repo.save(d);
    }

    @Transactional
    public Department reparent(UUID id, Department newParent) {
        var d = repo.findById(id).orElseThrow();
        d.setParent(newParent);     // бизнес-проверки на циклы/корень — при необходимости
        return repo.save(d);
    }

    @Transactional
    public void delete(UUID id) {
        repo.deleteById(id);        // поведение при удалении детей регулируется твоими триггерами в БД
    }
}
