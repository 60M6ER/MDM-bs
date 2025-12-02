package ru.baikalsr.backend.Exchange;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "exchange.cache")
@Getter
@Setter
public class ExchangeCacheProperties {
    /** максимальная длина очереди команд на устройство */
    private int maxQueueSizePerDevice = 200;
    /** TTL для requestId (идемпотентность), сек */
    private long requestIdTtlSec = 3600;
    /** через сколько секунд «протухает» heartbeat (для мониторинга), сек */
    private long heartbeatTtlSec = 3600;
    /** период фоновой очистки, сек */
    private long cleanupIntervalSec = 60;
    /** максимальное количество отчетов в очереди */
    private int maxReportsInQueue = 100000;
    /** максимальное количество сообщений о выполнении команды в очереди */
    private int maxAcksInQueue = 100000;
    /** через сколько секунд «протухает» report, сек */
    private long reportTtlSec = 3600;
    /** через сколько секунд «протухает» ack, сек */
    private long ackTtlSec = 3600;
}
