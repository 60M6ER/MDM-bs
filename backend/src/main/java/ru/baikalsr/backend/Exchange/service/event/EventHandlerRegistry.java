package ru.baikalsr.backend.Exchange.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.enums.DeviceEvents;
import ru.baikalsr.backend.Device.repository.DeviceRepository;
import ru.baikalsr.backend.Exchange.dto.EventItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EventHandlerRegistry {

    private final DeviceRepository deviceRepository;
    private final Map<DeviceEvents, EventHandler<?>> handlers;
    private final ObjectMapper om;

    @Autowired
    public EventHandlerRegistry(List<EventHandler<?>> handlers, ObjectMapper om, DeviceRepository deviceRepository) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(EventHandler::key, h -> h));
        this.om = om;
        this.deviceRepository = deviceRepository;
        log.info("Registered {} event handlers: {}", handlers.size(), this.handlers.keySet());
    }

    @SuppressWarnings("unchecked")
    public void dispatch(String deviceId, EventItem event) {
        var h = handlers.get(event.key());
        if (h == null) {
            log.warn("Not found registered handlers for event key - {}", event.key());
            return;
        }
        Device device = null;
        try {
            device = deviceRepository.findById(UUID.fromString(deviceId)).orElseThrow();
        } catch (Exception e) {
            log.warn("Not found device with id - {}", deviceId);
            return;
        }

        Object value;
        try {
            value = om.convertValue(event.payload(), h.type());
        } catch (IllegalArgumentException e) {
            log.error(
                    "Failed to convert payload for event {} from device {} to type {}",
                    event.key(), deviceId, h.type().getSimpleName(), e
            );
            return;
        }

        long ts = event.occurredAtEpochMs() != null
                ? event.occurredAtEpochMs()
                : System.currentTimeMillis();

        try {
            ((EventHandler<Object>) h).apply(device, value, ts);
        } catch (Exception e) {
            log.error(
                    "Failed to handle event {} from device {}",
                    event.key(), deviceId, e
            );
        }
    }
}
