package ru.baikalsr.backend.Security.dto;

public record AuthResponse(
        boolean success,          // авторизация прошла?
        String  message,          // человеко-читаемое сообщение
        String  code,             // машинный код результата (см. enum ниже)
        String  accessToken,      // при успехе
        Long    expiresIn,        // сек, при успехе
        String  tokenType         // "Bearer", при успехе
) {}
