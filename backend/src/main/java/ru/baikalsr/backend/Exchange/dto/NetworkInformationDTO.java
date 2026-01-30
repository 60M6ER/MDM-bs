package ru.baikalsr.backend.Exchange.dto;

import ru.baikalsr.backend.Device.enums.NetworkTypes;

public record NetworkInformationDTO(NetworkTypes typeNetwork, String wifiSsid) {
}
