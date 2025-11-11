package ru.baikalsr.backend.Exchange.dto;

public record AckDto(
        String id,            // ID команды (UUID)
        String status,        // OK | ERROR | SKIPPED | ...
        String message,       // текст ошибки / комментарий
        Long finishedAtEpochMs
) {}
