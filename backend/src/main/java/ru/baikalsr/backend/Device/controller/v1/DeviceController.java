package ru.baikalsr.backend.Device.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.baikalsr.backend.Device.dto.DeviceListItemDto;
import ru.baikalsr.backend.Device.dto.PreprovisionCreateResponse;
import ru.baikalsr.backend.Device.dto.DeviceRegisterByKeyRequest;
import ru.baikalsr.backend.Device.dto.DeviceRegisterResponse;
import ru.baikalsr.backend.Device.service.DeviceService;
import ru.baikalsr.backend.common.model.PageResponse;


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
            @PageableDefault(size = 30, sort = "deviceCreatedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(deviceService.findAll(pageable));
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
