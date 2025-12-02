package ru.baikalsr.backend.Exchange.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Device Exchange",
        description = "Точка обмена данными между устройством и MDM-сервером"
)
@RestController
@RequestMapping("/api/v1/device_exchange")
@RequiredArgsConstructor
public class DeviceExchangeController {
    private final ExchangeService service;

    @Operation(
            summary = "Получить команды от сервера и отправить состояние устройства",
            description = """
                    Устройство отправляет последнее состояние и события.
                    Сервер в ответ выдаёт пакет команд, которые необходимо выполнить.
                    
                    Идемпотентность обеспечивается через `X-Request-Id`.
                    `deviceId` берётся из access-token.
                    
                    Ответ:
                    * 200 — есть команды  
                    * 204 — команд нет (пустой пакет)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Команды для выполнения",
                    content = @Content(
                            schema = @Schema(implementation = DevicePullResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "Команд нет"
            ),
            @ApiResponse(responseCode = "400", description = "Неверный формат тела"),
            @ApiResponse(responseCode = "401", description = "Ошибка авторизации устройства")
    })
    @PostMapping("/pull")
    public ResponseEntity<DevicePullResponse> pull(
            @AuthenticationPrincipal DevicePrincipal principal,
            @Parameter(
                    description = "ID запроса для идемпотентности. "
                            + "При повторном вызове с тем же значением побочные эффекты не повторяются.",
                    required = false,
                    example = "c2dca1a3-4bde-4c77-a1b4-e55e5e0fbc12"
            )
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = """
                           Состояния и события, отправленные устройством.
                           deviceId берётся из токена, в теле отсутствует.
                           """,
                    content = @Content(schema = @Schema(implementation = DevicePullRequest.class))
            )
            @Valid @RequestBody DevicePullRequest body
    ) {
        var resp = service.pull(principal.deviceId(), requestId, body);
        return resp.commands().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Подтвердить выполнение команд",
            description = """
                    Устройство сообщает серверу о результате выполнения команд,
                    полученных ранее через `/pull`.

                    Позволяет серверу отслеживать прогресс, ошибки и статус применения политик.

                    Идемпотентность обеспечивается через `X-Request-Id`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Подтверждения приняты"
            ),
            @ApiResponse(responseCode = "400", description = "Неверный формат тела"),
            @ApiResponse(responseCode = "401", description = "Ошибка авторизации устройства")
    })
    @PostMapping("/ack")
    public ResponseEntity<Void> ack(
            @AuthenticationPrincipal DevicePrincipal principal,
            @Parameter(
                    description = "ID запроса для идемпотентности",
                    required = false,
                    example = "e410b95d-39bd-4d60-aebe-ece3bb761591"
            )
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Подтверждения выполнения серверных команд.",
                    content = @Content(schema = @Schema(implementation = AckRequest.class))
            )
            @Valid @RequestBody AckRequest body
    ) {
        service.ack(principal.deviceId(), requestId, body);
        return ResponseEntity.accepted().build();
    }
}
