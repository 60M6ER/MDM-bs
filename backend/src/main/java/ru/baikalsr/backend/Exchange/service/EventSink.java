package ru.baikalsr.backend.Exchange.service;

import ru.baikalsr.backend.Exchange.dto.EventItem;

import java.util.List;

public interface EventSink {
    void append(String deviceId, List<EventItem> events);
}
