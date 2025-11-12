package ru.baikalsr.backend.Device.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Setting.enums.SettingGroup;

@Getter
public class DeviceRegisteredEvent extends ApplicationEvent {
    Device  device;
    public DeviceRegisteredEvent(Object source, Device device) {
        super(source);
        this.device = device;
    }
}
