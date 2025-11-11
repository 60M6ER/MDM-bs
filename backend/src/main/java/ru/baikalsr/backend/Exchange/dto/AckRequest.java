package ru.baikalsr.backend.Exchange.dto;

import java.util.List;

public record AckRequest(
        List<AckDto> acks         // список подтверждений команд
) {
    public AckRequest {
        if (acks == null) acks = List.of();
    }
}
