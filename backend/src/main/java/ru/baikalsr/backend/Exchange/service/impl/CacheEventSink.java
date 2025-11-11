package ru.baikalsr.backend.Exchange.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Exchange.dto.EventItem;
import ru.baikalsr.backend.Exchange.service.EventSink;
import ru.baikalsr.backend.Exchange.service.ExchangeCache;

import java.util.List;

@Component
@RequiredArgsConstructor
class CacheEventSink implements EventSink {
    private final ExchangeCache cache; // либо outbox/БД
    @Override public void append(String deviceId, List<EventItem> events) {
        // сохрани как «последние события» или положи в очередь на асинхронную запись
    }
}
