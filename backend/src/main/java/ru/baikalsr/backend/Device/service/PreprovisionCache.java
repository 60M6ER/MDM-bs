package ru.baikalsr.backend.Device.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PreprovisionCache {

    public record Ticket(UUID preDeviceId, String regKey, Instant expiresAt, AtomicBoolean used) {}

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final char[] ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom rnd = new SecureRandom();
    private final ConcurrentHashMap<UUID, Ticket> map = new ConcurrentHashMap<>();

    public Ticket create() {
        UUID id = UUID.randomUUID();
        String key = generateKey(4) + "-" + generateKey(4) + "-" + generateKey(4);
        Instant exp = Instant.now().plus(TTL);
        Ticket t = new Ticket(id, key, exp, new AtomicBoolean(false));
        map.put(id, t);
        return t;
    }

    public Optional<Ticket> consume(UUID preDeviceId, String regKey) {
        Ticket t = map.get(preDeviceId);
        if (t == null) return Optional.empty();
        if (Instant.now().isAfter(t.expiresAt)) { map.remove(preDeviceId); return Optional.empty(); }
        if (!t.regKey.equals(regKey)) return Optional.empty();
        if (!t.used.compareAndSet(false, true)) return Optional.empty();
        map.remove(preDeviceId);
        return Optional.of(t);
    }

    public void cleanupExpired() {
        Instant now = Instant.now();
        map.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt) || e.getValue().used.get());
    }

    private String generateKey(int len) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) out[i] = ALPHANUM[rnd.nextInt(ALPHANUM.length)];
        return new String(out);
    }

    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        cleanupExpired();
    }
}
