package ru.baikalsr.backend.common.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.baikalsr.backend.common.dto.HealthCheckDto;

@RestController
@RequestMapping("/api/v1/health_check")
public class HealthController {

    @io.swagger.v3.oas.annotations.Operation(
            summary = "Проверка доступности сервера",
            description = "Возвращает ok=true, если backend запущен"
    )
    @org.springframework.web.bind.annotation.GetMapping
    public HealthCheckDto health() {
        return new HealthCheckDto(true, "ok");
    }
}
