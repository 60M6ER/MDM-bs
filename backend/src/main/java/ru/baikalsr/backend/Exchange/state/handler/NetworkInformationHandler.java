package ru.baikalsr.backend.Exchange.state.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.DeviceState;
import ru.baikalsr.backend.Device.enums.NetworkTypes;
import ru.baikalsr.backend.Device.enums.StateKey;
import ru.baikalsr.backend.Device.repository.DeviceStateRepository;
import ru.baikalsr.backend.Exchange.dto.NetworkInformationDTO;
import ru.baikalsr.backend.Exchange.state.StateHandler;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class NetworkInformationHandler implements StateHandler<NetworkInformationDTO> {
    private final DeviceStateRepository repo; // JPA/DAO куда пишем
    public StateKey key() { return StateKey.NETWORK_INFORMATION; }
    public Class<NetworkInformationDTO> type() { return NetworkInformationDTO.class; }

    @Transactional
    public void apply(String deviceId, NetworkInformationDTO dto, long at) {

        if (dto.typeNetwork() == NetworkTypes.WIFI && dto.wifiSsid().isBlank()) {
            log.warn("Wifi ssid was not set. device-{} ssid:{}", deviceId, dto.wifiSsid());
        } else {
            DeviceState deviceState = repo.findByDevice_Id(UUID.fromString(deviceId))
                    .orElse(new DeviceState(UUID.fromString(deviceId)));

            deviceState.setNetworkType(dto.typeNetwork());
            deviceState.setWifiSsid(dto.wifiSsid());

            repo.save(deviceState);
        }
    }
}
