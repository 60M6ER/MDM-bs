package ru.baikalsr.backend.Security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.baikalsr.backend.Setting.dto.AuthSettingsCfg;
import ru.baikalsr.backend.Setting.event.SettingsChangedEvent;
import ru.baikalsr.backend.Setting.enums.AuthProvider;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.service.SettingsService;
import ru.baikalsr.backend.User.service.AppUserDetailsService;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicLdapProvider implements AuthenticationProvider, ApplicationListener<SettingsChangedEvent> {

    private final SettingsService settings;
    private final AppUserDetailsService appUserDetailsService;

    /** Делегат, который реально аутентифицирует (LDAP/AD) или null если отключено */
    private volatile @Nullable AuthenticationProvider delegate;

    @PostConstruct
    private void startLogic() {
        rebuildIfNeeded();
    }

    private void rebuildIfNeeded() {
        AuthSettingsCfg cfg;
        try {
            cfg = settings.get(SettingGroup.AUTH_SETTINGS, AuthSettingsCfg.class);
        } catch (Exception e) {
            log.warn("AUTH_SETTINGS not found or invalid: {}", e.getMessage());
            delegate = null;
            return;
        }

        if (cfg == null || cfg.provider() == null) {
            delegate = null;
            return;
        }

        try {
            AuthProvider provider = cfg.provider();
            switch (provider) {
                case AD -> delegate = buildAdProvider(cfg);
                case LDAP -> delegate = buildGenericLdapProvider(cfg);
                case OIDC -> {
                    // Этот провайдер для LDAP/AD аутентификации.
                    // При OIDC аутентификация идёт через OAuth2Login (другой фильтр/провайдер).
                    delegate = null;
                }
            }
            log.info("DynamicLdapProvider rebuilt for provider={}", provider);
        } catch (Exception ex) {
            log.error("Failed to rebuild LDAP/AD provider: {}", ex.getMessage(), ex);
            delegate = null;
        }
    }

    /** Generic LDAP (OpenLDAP/FreeIPA/389-ds) */
    private AuthenticationProvider buildGenericLdapProvider(AuthSettingsCfg cfg) {
        var lc = cfg.ldap();
        if (lc == null || !StringUtils.hasText(lc.url())) {
            throw new IllegalArgumentException("LDAP.url must be set for provider=LDAP");
        }

        LdapContextSource cs = new LdapContextSource();
        cs.setUrl(lc.url());
        if (StringUtils.hasText(lc.baseDn())) cs.setBase(lc.baseDn());

        // В данной версии мы не используем bind DN/password.
        // Если понадобится поиск DN по фильтру на серверах без анонимного поиска —
        // добавим в DTO поля bindDn/bindPassword и установим их здесь.
        // cs.setUserDn(lc.bindDn());
        // cs.setPassword(lc.bindPassword());

        // Поведение referrals
        if (StringUtils.hasText(lc.referral())) {
            // LdapContextSource#setReferral не всегда доступен; зададим через environment если нужно
            cs.getBaseLdapPathAsString(); // no-op to ensure bean init
        }

        // TLS/Pooling/Timeouts — могут быть заданы через системные свойства JNDI
        // (com.sun.jndi.ldap.connect.timeout/read.timeout/connect.pool) или добавим позже из DTO.

        cs.afterPropertiesSet();

        BindAuthenticator authenticator = new BindAuthenticator(cs);

        if (lc.userDnPatterns() != null && !lc.userDnPatterns().isEmpty()) {
            authenticator.setUserDnPatterns(lc.userDnPatterns().toArray(String[]::new));
        } else if (StringUtils.hasText(lc.userSearchBase()) && StringUtils.hasText(lc.userSearchFilter())) {
            // ВАЖНО: Для этого режима сервер обычно требует bind DN/password для поиска.
            // Если анонимный поиск запрещён — доработаем DTO и contextSource.
            var userSearch = new FilterBasedLdapUserSearch(lc.userSearchBase(), lc.userSearchFilter(), cs);
            authenticator.setUserSearch(userSearch);
        } else {
            // дефолтный паттерн на случай отсутствия настроек
            authenticator.setUserDnPatterns(new String[]{"uid={0},ou=people"});
        }

        // Можно добавить LdapAuthoritiesPopulator при необходимости маппить группы → роли.
        return new LdapAuthenticationProvider(authenticator);
    }

    /** Active Directory (Microsoft) */
    private AuthenticationProvider buildAdProvider(AuthSettingsCfg cfg) {
        var ad = cfg.ad();
        if (ad == null || !StringUtils.hasText(ad.domain()) || ad.urls() == null || ad.urls().isEmpty()) {
            throw new IllegalArgumentException("AD settings must include domain and urls");
        }

        // AD провайдер принимает domain и первый URL (список можно склеить в пробельную строку — JNDI по очереди попробует)
        String urls = String.join(" ", ad.urls());
        var provider = new ActiveDirectoryLdapAuthenticationProvider(ad.domain(), urls);

        // convert sub-error codes to exceptions — удобнее диагностировать ошибки 49.xxx
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);

        // Можно обернуть в ProviderManager, если в будущем понадобится цепочка
        return provider;
    }

    @Override
    public void onApplicationEvent(SettingsChangedEvent event) {
        // Пересобираем только если изменилась группа AUTH_SETTINGS
        if (event.getSettingGroup() == SettingGroup.AUTH_SETTINGS) {
            rebuildIfNeeded();
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var d = delegate;
        if (d == null) return null; // передаём управление следующему провайдеру
        //return d.authenticate(authentication);
        try {
            // 1) Внешняя аутентификация против LDAP/AD
            Authentication ext = d.authenticate(authentication);
            String username = ext.getName();
            log.info("External LDAP/AD auth success for '{}'", username);

            // 2) Получаем displayName (если есть), иначе используем username
            String displayName = extractDisplayName(ext);

            // 3) Ищем пользователя у нас. Если нет — создаём с ROLE_USER
            boolean created = appUserDetailsService.ensureExistsWithDefaultRole(username, displayName);
            if (created) {
                log.info("Local user '{}' created with default ROLE_USER", username);
            }

            // 4) Грузим локальные authorities из нашей БД
            Collection<? extends GrantedAuthority> authorities = appUserDetailsService.loadAuthorities(username);

            // 5) Возвращаем успешную аутентификацию с нашими ролями
            return new UsernamePasswordAuthenticationToken(
                    ext.getPrincipal(),   // оставляем principal от внешнего провайдера
                    ext.getCredentials(), // пароль уже проверен
                    authorities           // но права — наши
            );
        } catch (AuthenticationException ex) {
            // 6) Логируем отказ и пробрасываем выше (пусть Spring решит результат)
            log.warn("External LDAP/AD auth failed for '{}': {}", authentication.getName(), ex.getMessage());
            //throw ex;
            return null;
        }
    }

    private String extractDisplayName(Authentication ext) {
        // Пытаемся вытащить читаемое имя, если провайдер его даёт.
        // Безопасный фоллбэк — username.
        try {
            Object principal = ext.getPrincipal();
            // Если это LDAP-детали, можно попробовать атрибуты:
            // Например, LdapUserDetailsImpl позволяет взять DN; displayName обычно недоступен здесь,
            // но некоторые кастомные провайдеры кладут его в details/attributes.
            var details = ext.getDetails();
            if (details instanceof org.springframework.ldap.core.DirContextAdapter ctx) {
                Object dn = ctx.getStringAttribute("displayName");
                if (dn instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignore) { }
        return ext.getName();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /** Служебное: активен ли провайдер (LDAP/AD) */
    public boolean isActive() { return delegate != null; }
}