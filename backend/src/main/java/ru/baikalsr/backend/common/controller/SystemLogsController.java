package ru.baikalsr.backend.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.baikalsr.backend.common.dto.LogChunkDto;
import ru.baikalsr.backend.common.dto.LogSearchResultDto;
import ru.baikalsr.backend.common.service.LogViewerService;

@RestController
@RequestMapping("/api/v1/system/logs")
@RequiredArgsConstructor
@Tag(name = "System Logs", description = "Просмотр backend-логов из файлового хранилища.")
public class SystemLogsController {

    private final LogViewerService logViewerService;

    @GetMapping
    @Operation(summary = "Получить фрагмент логов")
    public LogChunkDto getLogs(
            @RequestParam(defaultValue = "latest") String mode,
            @RequestParam(required = false) Long line,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) Integer before,
            @RequestParam(required = false) Integer after
    ) {
        return switch (mode) {
            case "latest" -> logViewerService.readLatest(limit);
            case "older" -> logViewerService.readOlder(line != null ? line : 1, limit);
            case "newer" -> logViewerService.readNewer(line != null ? line : 0, limit);
            case "around" -> logViewerService.readAround(line != null ? line : 1, before, after);
            default -> logViewerService.readLatest(limit);
        };
    }

    @GetMapping("/search")
    @Operation(summary = "Найти строки логов по подстроке")
    public LogSearchResultDto search(
            @RequestParam String query,
            @RequestParam(defaultValue = "500") int limit
    ) {
        return logViewerService.search(query, limit);
    }
}
