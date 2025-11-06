package ru.baikalsr.backend.Setting.dto;

import ru.baikalsr.backend.Setting.enums.AuthProvider;

import java.util.List;

public record AuthSettingsCfg(
        AuthProvider provider,
        AdCfg ad,
        LdapCfg ldap,
        OidcCfg oidc
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
