package ru.baikalsr.backend.Device.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.baikalsr.backend.Device.dto.DeviceEventDto;
import ru.baikalsr.backend.Device.mapper.DeviceEventsMapper;
import ru.baikalsr.backend.Device.service.DeviceEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/events")
@RequiredArgsConstructor
public class DeviceEventsController {

    private  final DeviceEventService deviceEventService;

    @GetMapping
    @Operation(summary = "Получить список событий устройства (пагинация, последние первыми)")
    public Page<DeviceEventDto> getDeviceEvents(
            @Parameter(description = "ID устройства") @PathVariable UUID deviceId,
            @PageableDefault(size = 30)
            Pageable pageable
    ) {
        return DeviceEventsMapper.toDtoPage(deviceEventService.getPage(deviceId, pageable));
    }
}
