package ru.baikalsr.backend.Setting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Setting.dto.AuthSettingsCfg;
import ru.baikalsr.backend.Setting.entity.SettingEntity;
import ru.baikalsr.backend.Setting.event.SettingsChangedEvent;
import ru.baikalsr.backend.Setting.enums.AuthProvider;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.repository.SettingsRepository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SettingsService {
    private final SettingsRepository settingsRepository;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    // Простой локальный кэш (можно заменить на Caffeine / Spring Cache позже)
    private final Map<SettingGroup, Object> cache = new ConcurrentHashMap<>();

    /**
     * Получает настройки заданной группы, десериализует в указанный тип и кэширует.
     */
    public <T> T get(SettingGroup group, Class<T> type) {
        Object cached = cache.get(group);
        if (cached != null) return type.cast(cached);

        var entityOpt = settingsRepository.findById(group.name());
        if (entityOpt.isEmpty()) {
            // Нет записи — подсовываем дефолт для известных групп
            if (group == SettingGroup.AUTH_SETTINGS && type == AuthSettingsCfg.class) {
                T dto = type.cast(defaultAuthSettings());
                cache.put(group, dto);
                log.info("Settings '{}' not found, using defaults (disabled)", group);
                return dto;
            }
            // для неизвестных групп пока бросаем, чтобы не скрыть ошибки разработки
            throw new IllegalStateException("Setting " + group + " not found");
        }

        try {
            T dto = mapper.treeToValue(entityOpt.get().getValue(), type);
            cache.put(group, dto);
            return dto;
        } catch (Exception e) {
            // Битый JSON — тоже дефолт для AUTH_SETTINGS
            if (group == SettingGroup.AUTH_SETTINGS && type == AuthSettingsCfg.class) {
                T dto = type.cast(defaultAuthSettings());
                cache.put(group, dto);
                log.warn("Failed to parse AUTH_SETTINGS, using defaults (disabled): {}", e.getMessage());
                return dto;
            }
            throw new RuntimeException("Failed to parse settings for " + group, e);
        }
    }

    /**
     * Загружает при страте приложения настройки из БД, чтобы сервисы могли их получить.
     * Если загрузить не удалось, то генерирует объекты по умолчанию и кладет в кэш.
     */
    @PostConstruct
    public void preload() {
        // AUTH_SETTINGS
        settingsRepository.findById(SettingGroup.AUTH_SETTINGS.name()).ifPresentOrElse(entity -> {
            try {
                AuthSettingsCfg cfg = mapper.treeToValue(entity.getValue(), AuthSettingsCfg.class);
                cache.put(SettingGroup.AUTH_SETTINGS, cfg);
                log.info("Preloaded settings group: {}", SettingGroup.AUTH_SETTINGS);
            } catch (Exception ex) {
                cache.put(SettingGroup.AUTH_SETTINGS, defaultAuthSettings());
                log.warn("Failed to parse AUTH_SETTINGS at startup; using defaults (disabled): {}", ex.getMessage());
            }
        }, () -> {
            cache.put(SettingGroup.AUTH_SETTINGS, defaultAuthSettings());
            log.info("AUTH_SETTINGS not found at startup; using defaults (disabled)");
        });
    }

    /**
     * Сохраняет настройки: сериализует DTO → JSON, обновляет запись в БД и кэш.
     */
    @Transactional
    public <T> void save(SettingGroup group, T payload) {
        if (group == SettingGroup.AUTH_SETTINGS && payload instanceof AuthSettingsCfg authCfg) {
            validateAuthSettings(authCfg);
        }

        try {
            //String json = mapper.writeValueAsString(payload);
            JsonNode json = mapper.valueToTree(payload);

            SettingEntity entity = settingsRepository.findById(group.name())
                    .orElseGet(() -> SettingEntity.builder()
                            .name(group.name())
                            .value(json)
                            .version(1)
                            .updatedAt(Instant.now())
                            .build());

            entity.setValue(json);
            entity.setUpdatedAt(Instant.now());
            settingsRepository.save(entity);
            cache.put(group, payload);
            log.info("Updated settings group: {}", group);
            // уведомим подписчиков (LDAP/AD провайдер и пр.)
            eventPublisher.publishEvent(new SettingsChangedEvent(this, group));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save settings for " + group, e);
        }
    }

    /**
     * Сбрасывает запись из локального кэша.
     */
    public void invalidate(SettingGroup group) {
        cache.remove(group);
    }

    /**
     * Простая логическая валидация настроек авторизации.
     */
    private void validateAuthSettings(AuthSettingsCfg cfg) {
        if (cfg.provider() == null) {
            throw new IllegalArgumentException("Auth provider must be specified");
        }

        AuthProvider provider = cfg.provider();
        switch (provider) {
            case AD -> {
                if (cfg.ad() == null || cfg.ad().domain() == null || cfg.ad().urls() == null || cfg.ad().urls().isEmpty()) {
                    throw new IllegalArgumentException("AD settings must include domain and at least one URL");
                }
            }
            case LDAP -> {
                if (cfg.ldap() == null || cfg.ldap().baseDn() == null) {
                    throw new IllegalArgumentException("LDAP settings must include baseDn");
                }
            }
            case OIDC -> {
                if (cfg.oidc() == null || cfg.oidc().issuer() == null) {
                    throw new IllegalArgumentException("OIDC settings must include issuer");
                }
            }
        }
    }

    private AuthSettingsCfg defaultAuthSettings() {
        // provider=null → провайдер не выбран, внешняя авторизация выключена
        return new AuthSettingsCfg(null, null, null, null);
    }
}
