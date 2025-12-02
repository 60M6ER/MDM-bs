package ru.baikalsr.backend.Exchange.state.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.DeviceState;
import ru.baikalsr.backend.Device.repository.DeviceStateRepository;
import ru.baikalsr.backend.Exchange.dto.StateKey;
import ru.baikalsr.backend.Exchange.state.StateHandler;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class BatteryPercentHandler implements StateHandler<Byte> {
    private final DeviceStateRepository repo; // JPA/DAO куда пишем
    public StateKey key() { return StateKey.BATTERY_INFORMATION; }
    public Class<Byte> type() { return Byte.class; }

    @Transactional
    public void apply(String deviceId, Byte value, long at) {
        if (value < 0 || value > 100) {
            log.warn("Battery percent value out of range {} - [{}, {}]", value, 0, 100);
        } else {
            DeviceState deviceState = repo.findByDevice_Id(UUID.fromString(deviceId))
                    .orElse(new DeviceState());

            deviceState.setBatteryLevel(value);
            repo.save(deviceState);
        }
    }
}
