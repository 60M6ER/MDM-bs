package ru.baikalsr.backend.Setting.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import ru.baikalsr.backend.Setting.enums.AuthProvider;

import java.util.List;

public record AuthSettingsCfg(
        @NotNull AuthProvider provider,
        @Nullable AdCfg ad,
        @Nullable LdapCfg ldap,
        @Nullable OidcCfg oidc
) {
    public record AdCfg(String domain, List<String> urls, String referral) {}
    public record LdapCfg(
            String url,
            String baseDn,
            // Пользователь: либо patterns[], либо (base+filter)
            List<String> userDnPatterns,
            String userSearchBase,
            String userSearchFilter,
            // Группы (опционально)
            String groupSearchBase,
            String groupSearchFilter,
            // Транспорт/поведение (опционально)
            String referral, Boolean startTls,
            String trustStorePath, String trustStorePassword,
            Integer connectTimeoutMs, Integer readTimeoutMs, Boolean poolEnabled
    ) {}
    public record OidcCfg(String issuer, String clientId, String clientSecret, String redirectUri, String groupsClaim) {}
}
