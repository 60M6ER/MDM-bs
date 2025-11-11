package ru.baikalsr.backend.Exchange.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Exchange.dto.StateKey;
import ru.baikalsr.backend.Exchange.dto.StateUpdate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StateHandlerRegistry {
    private final Map<StateKey, StateHandler<?>> handlers;

    @Autowired
    public StateHandlerRegistry(List<StateHandler<?>> list) {
        this.handlers = list.stream().collect(Collectors.toMap(StateHandler::key, h -> h));
    }

    @Autowired private ObjectMapper om;

    @SuppressWarnings("unchecked")
    public void dispatch(String deviceId, StateUpdate update) {
        var h = handlers.get(update.key());
        if (h == null) return; // или throw, если хотим жёстко
        var value = om.convertValue(update.value(), h.type());
        var ts = update.observedAtEpochMs() != null ? update.observedAtEpochMs() : System.currentTimeMillis();
        ((StateHandler<Object>) h).apply(deviceId, value, ts);
    }
}
