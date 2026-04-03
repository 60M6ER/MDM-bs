package ru.baikalsr.backend.Exchange.state.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.enums.StateKey;
import ru.baikalsr.backend.Device.service.DeviceStateService;
import ru.baikalsr.backend.Exchange.state.StateHandler;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class KioskIsOnHandler implements StateHandler<Boolean> {
    private final DeviceStateService deviceStateService;
    public StateKey key() { return StateKey.KIOSK_IS_ON; }
    public Class<Boolean> type() { return Boolean.class; }

    @Transactional
    public void apply(String deviceId, Boolean value, long at) {
        deviceStateService.upsertByDeviceId(UUID.fromString(deviceId), state -> state.setKioskIsOn(value));
    }
}
