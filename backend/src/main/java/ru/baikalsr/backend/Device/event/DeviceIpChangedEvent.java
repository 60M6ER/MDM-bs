package ru.baikalsr.backend.Device.event;

import org.springframework.context.ApplicationEvent;
import ru.baikalsr.backend.Device.entity.Device;

public class DeviceIpChangedEvent extends ApplicationEvent {
    Device device;
    public DeviceIpChangedEvent(Object source, Device device) {
        super(source);
        this.device = device;
    }
}
