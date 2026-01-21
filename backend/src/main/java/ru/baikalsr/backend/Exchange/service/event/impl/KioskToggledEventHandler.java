package ru.baikalsr.backend.Exchange.service.event.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.enums.DeviceEvents;
import ru.baikalsr.backend.Device.service.DeviceEventService;
import ru.baikalsr.backend.Exchange.dto.BooleanEventDto;
import ru.baikalsr.backend.Exchange.service.event.EventHandler;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class KioskToggledEventHandler implements EventHandler<BooleanEventDto> {

    private final DeviceEventService deviceEventService;

    @Override
    public DeviceEvents key() {
        return DeviceEvents.KIOSK_MODE_TOGGLED; // твой enum
    }

    @Override
    public Class<BooleanEventDto> type() {
        return BooleanEventDto.class;
    }

    @Override
    public void apply(Device device, BooleanEventDto payload, long occurredAtMs) {
        log.info("Handle SOME_EVENT from device {} at {}: {}", device.getId(), occurredAtMs, payload);
        DeviceEvents deviceEvent;
        if (payload.value()) {
            deviceEvent = DeviceEvents.KIOSK_MODE_TOGGLED_ON;
        } else {
            deviceEvent = DeviceEvents.KIOSK_MODE_TOGGLED_OFF;
        }
        deviceEventService.append(device, deviceEvent, Instant.ofEpochMilli(occurredAtMs));
    }
}
