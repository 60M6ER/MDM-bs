package ru.baikalsr.backend.Device.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.baikalsr.backend.Setting.dto.ExchangeSettingsCfg;
import ru.baikalsr.backend.Setting.enums.SettingGroup;
import ru.baikalsr.backend.Setting.service.SettingsService;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStateRepository deviceStateRepository;
    private final DeviceDetailsCurrentRepository deviceDetailsCurrentRepository;
    private final DeviceDetailsMapper deviceDetailsMapper;
    private final DeviceSecretRepository deviceSecretRepository;
    private final DeviceLastSeenService deviceLastSeenService;
    private final PreprovisionCache preprovisionCache;
    private final PasswordEncoder passwordEncoder;
    private final DeviceEventRepository deviceEventRepository;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

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

    public PreprovisionCreateResponse createPreprovision(HttpServletRequest request) {
        var t = preprovisionCache.create();
        Map<String, Object> qrPayload = new HashMap<>();

        // 1. Базовый payload (системные поля)
        Map<String, Object> basePreprovisionMap = getBasePreprovisionMap(request, t);

        // 2. Забираем настройки обмена
        ExchangeSettingsCfg exchangeCfg =
                settingsService.get(SettingGroup.EXCHANGE_SETTINGS, ExchangeSettingsCfg.class);
        //qrPayload.put("exchangePeriodSec", exchangeCfg.getExchangePeriodSec());
        // 3. Если в настройках есть текст для QR — пробуем распарсить
        if (exchangeCfg.getQrPayloadText() != null && !exchangeCfg.getQrPayloadText().isBlank()) {
            try {
                Map<String, Object> customPayload =
                        objectMapper.readValue(
                                exchangeCfg.getQrPayloadText(),
                                new TypeReference<Map<String, Object>>() {}
                        );

                // кладём пользовательские поля первыми
                qrPayload.putAll(customPayload);

                if (qrPayload.containsKey("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE")) {
                    Map<String, Object> PROVISIONING_ADMIN_EXTRAS_BUNDLE = (Map<String, Object>) qrPayload.get("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE");
                    PROVISIONING_ADMIN_EXTRAS_BUNDLE.putAll(basePreprovisionMap);
                }

            } catch (Exception e) {
                // принципиально НЕ падаем — QR всё равно должен быть выдан
                log.warn("Failed to parse exchange QR payload template, ignoring it: {}", e.getMessage());
            }
        } else {
            // TODO: Make exception and server answer
            //qrPayload.put("error", "QR Payload text is missing");
        }


        return new PreprovisionCreateResponse(
                t.preDeviceId(),
                t.regKey(),
                t.expiresAt(),
                qrPayload
        );
    }

    private static Map<String, Object> getBasePreprovisionMap(HttpServletRequest request, PreprovisionCache.Ticket t) {
        String serverURL = request.getScheme() +
                "://" +
                request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? ""
                        : ":" + request.getServerPort());

        Map<String, Object> qrPayload = new HashMap<>();
        qrPayload.put("EXTRA_REGISTRATION_PRE_DEVICE_ID", t.preDeviceId().toString());
        qrPayload.put("EXTRA_REGISTRATION_REG_KEY", t.regKey());
        qrPayload.put("EXTRA_REGISTRATION_SERVER_URL", serverURL);
        qrPayload.put("EXTRA_REGISTRATION_ENDPOINT", "/api/v1/devices/register");
        return qrPayload;
    }

    @Transactional
    public void deleteDevice(UUID deviceId) {
        deviceRepository.deleteById(deviceId);
    }

    @Transactional
    public DeviceRegisterResponse registerFromPreprovision(DeviceRegisterByKeyRequest req, HttpServletRequest httpRequest) {
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

        device = deviceRepository.save(device);

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

        deviceLastSeenService.updateLastSeenIpAsync(device.getId(), httpRequest);

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
