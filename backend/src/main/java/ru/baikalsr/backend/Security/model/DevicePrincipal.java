package ru.baikalsr.backend.Security.model;

import java.security.Principal;

public record DevicePrincipal(String deviceId) implements Principal {

    @Override
    public String getName() {
        return deviceId;
    }
}
