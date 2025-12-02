package ru.baikalsr.backend.Exchange.dto;

public record BatteryInformationDTO(
    Boolean isCharging,
    Byte percent,
    Float voltage,
    Float temperature
) {
}
