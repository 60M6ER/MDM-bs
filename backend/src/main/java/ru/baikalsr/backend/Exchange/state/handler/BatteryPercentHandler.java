package ru.baikalsr.backend.Exchange.state.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Device.repository.DeviceStateRepository;
import ru.baikalsr.backend.Exchange.dto.StateKey;
import ru.baikalsr.backend.Exchange.state.StateHandler;

@Component
@RequiredArgsConstructor
class BatteryPercentHandler implements StateHandler<Integer> {
    private final DeviceStateRepository repo; // JPA/DAO куда пишем
    public StateKey key() { return StateKey.BATTERY_PERCENT; }
    public Class<Integer> type() { return Integer.class; }

    public void apply(String deviceId, Integer value, long at) {
        //repo.upsertBattery(deviceId, value, at); // твоя реализация
    }
}
