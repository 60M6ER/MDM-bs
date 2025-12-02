package ru.baikalsr.backend.Device.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.baikalsr.backend.Device.dto.*;
import ru.baikalsr.backend.Device.entity.*;
import ru.baikalsr.backend.Device.enums.DeviceEvents;
import ru.baikalsr.backend.Device.enums.DeviceStatus;
import ru.baikalsr.backend.Device.mapper.DeviceDetailsMapper;
import ru.baikalsr.backend.Device.repository.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStateRepository deviceStateRepository;
    private final DeviceDetailsCurrentRepository deviceDetailsCurrentRepository;
    private final DeviceDetailsMapper deviceDetailsMapper;
    private final DeviceSecretRepository deviceSecretRepository;
    private final PreprovisionCache preprovisionCache;
    private final PasswordEncoder passwordEncoder;
    private final DeviceEventRepository deviceEventRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Page<DeviceListItemDto> findAll(Pageable pageable) {
        return deviceDetailsMapper.toListItems(deviceRepository.findAll(pageable));
    }

    public Optional<Device> findById(UUID id) {
        return deviceRepository.findById(id);
    }

    public Optional<Device>  findBySerialNumber(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber);
    }

    public DeviceDetailsDto getDetails(UUID deviceId) {
        DeviceDetailsCurrent v = deviceDetailsCurrentRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND"));
        return deviceDetailsMapper.toDetails(v);
    }

    @Transactional
    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    @Transactional
    public void delete(UUID id) {
        deviceRepository.deleteById(id);
    }

    public boolean existsBySerialNumber(String serialNumber) {
        return deviceRepository.existsBySerialNumber(serialNumber);
    }

    public PreprovisionCreateResponse createPreprovision() {
        var t = preprovisionCache.create();
        var qrPayload = Map.<String, Object>of(
                "v", 1,
                "preDeviceId", t.preDeviceId().toString(),
                "regKey", t.regKey(),
                // относительный эндпоинт — фронт сам подставит базовый URL
                "endpoint", "/api/v1/devices/register"
        );
        return new PreprovisionCreateResponse(
                t.preDeviceId(),
                t.regKey(),
                t.expiresAt(),
                qrPayload
        );
    }

    @Transactional
    public DeviceRegisterResponse registerFromPreprovision(DeviceRegisterByKeyRequest req) {
        // 1) Проверка ключа предрегистрации (одноразовый, с TTL)
        var ticketOpt = preprovisionCache.consume(req.preDeviceId(), req.regKey());
        if (ticketOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REG_KEY_INVALID");
        }

//        // 2) Проверки уникальности
//        if (deviceRepository.existsById(req.preDeviceId())) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "DEVICE_ID_ALREADY_REGISTERED");
//        }

        // 3) Генерация секрета (plain → отдать клиенту, hash → хранить)
        String secretPlain = generateDeviceSecret(32);
        String secretHash = passwordEncoder.encode(secretPlain);

        // 4) Создание Device
        Instant now = Instant.now();
        Device device = null;
        // Проверяем созданные в бд устройства. если по серийному номеру будет найдено устройство то заменим deviceId
        device = deviceRepository.findBySerialNumber(req.serial()).orElse(new Device());

        if (device.getId() == null){
            device.setId(req.preDeviceId());              // фиксируем preDeviceId как окончательный deviceId
            device.setSerialNumber(req.serial());
            device.markNew();
            device.setCreatedAt(now);
        }

        device.setManufacturer(req.manufacturer());
        device.setModel(req.model());

        device.setStatus(DeviceStatus.ACTIVE);
        device.setEnrolledAt(now);
        device.setUpdatedAt(now);

        deviceRepository.save(device);

        DeviceSecret deviceSecret = deviceSecretRepository.findByDeviceId(device.getId()).orElse(new DeviceSecret());
        if (deviceSecret.getId() == null){
            deviceSecret.setDeviceId(device.getId());
            deviceSecret.markNew();
        }
        deviceSecret.setSecretHash(secretHash);
        deviceSecret.setCreatedUtc(now);

        deviceSecretRepository.save(deviceSecret);

        // 5) Инициализация состояния
        DeviceState deviceState = deviceStateRepository.findByDevice_Id(device.getId()).orElse(new DeviceState());
        deviceState.setDevice(device);
        deviceState.setAppVersion(req.appVersion());
        deviceState.setOsVersion(req.osVersion());
        deviceState.setUpdatedAt(now);
        deviceStateRepository.save(deviceState);

        DeviceEvent registeredEvent = DeviceEvent.builder()
                .device(device)
                .event(DeviceEvents.REGISTERED)
                .occurredAt(now)
                .build();
        deviceEventRepository.save(registeredEvent);

        // 6) Ответ — секрет возвращаем один раз
        return new DeviceRegisterResponse(device.getId(), secretPlain, 0);
    }

    public boolean matchesSecret(Device device, String secretHeader) {
        return deviceSecretRepository.findByDeviceId(device.getId())
                .map(deviceSecret -> passwordEncoder.matches(secretHeader, deviceSecret.getSecretHash()))
                .orElse(false);
    }

    private static String generateDeviceSecret(int bytesLen) {
        byte[] bytes = new byte[bytesLen];
        SECURE_RANDOM.nextBytes(bytes);
        // url-safe, без паддинга
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
