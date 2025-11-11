package ru.baikalsr.backend.Device.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import ru.baikalsr.backend.Device.dto.*;
import ru.baikalsr.backend.Device.service.DeviceService;
import ru.baikalsr.backend.common.model.PageResponse;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(
        name = "Devices",
        description = "Операции для работы со списком устройств и детальной информацией"
)
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(
            summary = "Получить список устройств",
            description = """
                    Возвращает постраничный список устройств с возможностью фильтрации.
                    
                    Фильтры работают по логике AND (все указанные условия должны выполняться).
                    Параметры пагинации — стандартные Spring (page, size, sort).
                    """
    )
    public PageResponse<DeviceListItemDto> getDevices(
            @ParameterObject
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(deviceService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить подробную информацию об устройстве по ID",
            description = """
                    Возвращает сводную информацию текущих состояний устройства, собранную со всех таблиц, связанных с устройством.
                    """
    )
    public DeviceDetailsDto getDeviceDetails(@PathVariable("id") UUID id) {
        return deviceService.getDetails(id);
    }

    @PostMapping("/preprovision")
    @Operation(
            summary = "Создать предрегистрацию устройства (QR)",
            description = """
                    Генерирует одноразовый ключ и preDeviceId для регистрации устройства через QR.
                    Возвращает payload, который можно зашить в QR-код.
                    Доступ: администратор.
                    """
    )
    public PreprovisionCreateResponse createPreprovision() {
        return deviceService.createPreprovision();
    }

    @PostMapping("/register")
    @Operation(
            summary = "Зарегистрировать устройство по QR-ключу",
            description = """
                    Принимает preDeviceId и regKey, а также технические данные устройства.
                    При успешной проверке ключа создаёт запись устройства и возвращает deviceId и deviceSecret.
                    """
    )
    public DeviceRegisterResponse registerDevice(
            @RequestBody DeviceRegisterByKeyRequest request
    ) {
        return deviceService.registerFromPreprovision(request);
    }
}
