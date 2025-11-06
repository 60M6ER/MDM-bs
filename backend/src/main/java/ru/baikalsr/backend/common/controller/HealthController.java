package ru.baikalsr.backend.common.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health_check")
public class HealthController {

    @io.swagger.v3.oas.annotations.Operation(
            summary = "Проверка доступности сервера",
            description = "Возвращает ok=true, если backend запущен"
    )
    @org.springframework.web.bind.annotation.GetMapping
    public java.util.Map<String, Object> health() {
        return java.util.Map.of(
                "ok", true,
                "message", "OK"
        );
    }
}
