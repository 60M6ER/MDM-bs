package ru.baikalsr.backend.common.dto;

public record HealthCheckDto(boolean ok, String message) {
}
