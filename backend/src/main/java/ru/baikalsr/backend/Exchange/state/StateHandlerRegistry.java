package ru.baikalsr.backend.Exchange.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Device.enums.StateKey;
import ru.baikalsr.backend.Exchange.dto.StateUpdate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StateHandlerRegistry {
    private final Map<StateKey, StateHandler<?>> handlers;
    private final ObjectMapper om;

    @Autowired
    public StateHandlerRegistry(List<StateHandler<?>> list,  ObjectMapper om) {
        this.handlers = list.stream().collect(Collectors.toMap(StateHandler::key, h -> h));
        this.om = om;
    }

    @SuppressWarnings("unchecked")
    public void dispatch(String deviceId, StateUpdate update) {
        var h = handlers.get(update.key());
        if (h == null) {
            log.warn("Not found registered handlers for key state - {}", update.key());
            return;
        }
        var value = om.convertValue(update.value(), h.type());
        var ts = update.observedAtEpochMs() != null ? update.observedAtEpochMs() : System.currentTimeMillis();
        ((StateHandler<Object>) h).apply(deviceId, value, ts);
    }
}
