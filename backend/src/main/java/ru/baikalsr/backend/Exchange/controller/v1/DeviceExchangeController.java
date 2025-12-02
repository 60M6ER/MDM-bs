package ru.baikalsr.backend.Exchange.controller.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.baikalsr.backend.Exchange.dto.AckRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullRequest;
import ru.baikalsr.backend.Exchange.dto.DevicePullResponse;
import ru.baikalsr.backend.Exchange.service.ExchangeService;
import ru.baikalsr.backend.Security.model.DevicePrincipal;

@RestController
@RequestMapping("/api/v1/device_exchange")
@RequiredArgsConstructor
public class DeviceExchangeController {
    private final ExchangeService service;

    @PostMapping("/pull")
    public ResponseEntity<DevicePullResponse> pull(
            @AuthenticationPrincipal DevicePrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody DevicePullRequest body
    ) {
        var resp = service.pull(principal.deviceId(), requestId, body);
        return resp.commands().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(resp);
    }


    @PostMapping("/ack")
    public ResponseEntity<Void> ack(
            @AuthenticationPrincipal DevicePrincipal principal,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody AckRequest body
    ) {
        service.ack(principal.deviceId(), requestId, body);
        return ResponseEntity.accepted().build();
    }
}
