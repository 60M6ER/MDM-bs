package ru.baikalsr.backend.Security.service;

import ru.baikalsr.backend.Security.model.AuthResult;

public interface AuthService {
    AuthResult authenticate(String username, String password);
}
