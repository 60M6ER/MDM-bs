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
class KioskIsOnHandler implements StateHandler<Boolean> {
    private final DeviceStateRepository repo; // JPA/DAO куда пишем
    public StateKey key() { return StateKey.KIOSK_IS_ON; }
    public Class<Boolean> type() { return Boolean.class; }

    @Transactional
    public void apply(String deviceId, Boolean value, long at) {
        DeviceState deviceState = repo.findByDevice_Id(UUID.fromString(deviceId))
                .orElse(new DeviceState());

        deviceState.setKioskIsOn(value);
        repo.save(deviceState);
    }
}
