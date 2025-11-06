package ru.baikalsr.backend.Security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import ru.baikalsr.backend.Security.dto.AuthResponse;
import ru.baikalsr.backend.Security.dto.LoginRequest;
import ru.baikalsr.backend.Security.model.AuthResult;
import ru.baikalsr.backend.Security.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authorization endpoints", description = "Точки для авторизации пользователей и устройств")
public class AuthController {

    private final AuthService auth;


    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Авторизоваться по паре логин/пароль",
            description = "Выполняет проверку по паре логин/пароль. Если проверка пройдена успешно отдает Access Token и Refresh Token."
    )
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        AuthResult r = auth.authenticate(req.username(), req.password());
        var body = new AuthResponse(true, "Вход выполнен", "OK",
                r.jwt(), r.ttlSec(), "Bearer");
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthResponse> badCreds(BadCredentialsException ex) {
        var body = new AuthResponse(false, "Неверный логин или пароль",
                "INVALID_CREDENTIALS", null, null, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> anyError(Exception ex) {
        var body = new AuthResponse(false, "Внутренняя ошибка. Попробуйте позже",
                "INTERNAL_ERROR", null, null, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
