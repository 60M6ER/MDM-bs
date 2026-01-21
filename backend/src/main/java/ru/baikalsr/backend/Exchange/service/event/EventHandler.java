package ru.baikalsr.backend.Exchange.service.event;

import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.enums.DeviceEvents;

/**
 * Обработчик событий от устройств.
 * @param <T> тип полезной нагрузки события после конвертации из JsonNode
 */
public interface EventHandler<T> {

    /**
     * Какой ключ события этот хендлер обрабатывает.
     */
    DeviceEvents key();

    /**
     * В какой тип приводим payload из EventItem.payload().
     */
    Class<T> type();

    /**
     * Основная логика обработки.
     *
     * @param device          Объект устройства из БД
     * @param payload         уже приведённый к нужному типу объект
     * @param occurredAtMs    когда событие произошло (из EventItem.occurredAtEpochMs(),
     *                        либо текущее время, если там null)
     */
    void apply(Device device, T payload, long occurredAtMs);
}
