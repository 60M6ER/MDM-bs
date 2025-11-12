package ru.baikalsr.backend.Device.mapper;

import ru.baikalsr.backend.Device.dto.DeviceEventDto;
import ru.baikalsr.backend.Device.entity.DeviceEvent;
import org.springframework.data.domain.Page;

public class DeviceEventsMapper {

    public static DeviceEventDto toDto(DeviceEvent entity) {
        if (entity == null) return null;
        return new DeviceEventDto(
                entity.getId(),
                entity.getEvent(),
                entity.getOccurredAt()
        );
    }

    public static Page<DeviceEventDto> toDtoPage(Page<DeviceEvent> page) {
        return page.map(DeviceEventsMapper::toDto);
    }
}
