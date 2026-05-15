package ru.baikalsr.backend.Security;

import org.springframework.util.StringUtils;

import java.util.Locale;

public final class LdapUrlNormalizer {

    private LdapUrlNormalizer() {
    }

    public static String normalize(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalArgumentException("LDAP URL is empty");
        }

        String value = rawUrl.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        if (lower.startsWith("ldap://")) {
            return "ldap://" + value.substring(7);
        }
        if (lower.startsWith("ldaps://")) {
            return "ldaps://" + value.substring(8);
        }

        throw new IllegalArgumentException("LDAP URL must start with ldap:// or ldaps://");
    }
}
