package ru.baikalsr.backend.Setting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.baikalsr.backend.Setting.entity.SettingEntity;

public interface SettingsRepository extends JpaRepository<SettingEntity, String> {}
