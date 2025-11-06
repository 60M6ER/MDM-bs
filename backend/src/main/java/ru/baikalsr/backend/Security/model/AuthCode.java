package ru.baikalsr.backend.Security.model;

public enum AuthCode {
    OK, INVALID_CREDENTIALS, ACCOUNT_LOCKED, ACCOUNT_DISABLED, LDAP_DOWN, INTERNAL_ERROR
}
