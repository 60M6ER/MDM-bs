package ru.baikalsr.backend.Exchange.state.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Exchange.dto.BatteryInformationDTO;
import ru.baikalsr.backend.Device.enums.StateKey;
import ru.baikalsr.backend.Device.service.DeviceStateService;
import ru.baikalsr.backend.Exchange.state.StateHandler;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class BatteryInformationHandler implements StateHandler<BatteryInformationDTO> {
    private final DeviceStateService deviceStateService;
    public StateKey key() { return StateKey.BATTERY_INFORMATION; }
    public Class<BatteryInformationDTO> type() { return BatteryInformationDTO.class; }

    @Transactional
    public void apply(String deviceId, BatteryInformationDTO dto, long at) {
        if (dto.percent() < 0 || dto.percent() > 100) {
            log.warn("Battery percent value out of range {} - [{}, {}]", dto.percent(), 0, 100);
        } else {
            deviceStateService.upsertByDeviceId(UUID.fromString(deviceId), state -> {
                state.setCharging(dto.isCharging());
                state.setBatteryLevel(dto.percent());
                state.setBatteryVoltage(dto.voltage());
                state.setBatteryTemperature(dto.temperature());
            });
        }
    }
}
