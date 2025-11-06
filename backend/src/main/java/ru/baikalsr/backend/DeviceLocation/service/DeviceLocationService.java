package ru.baikalsr.backend.DeviceLocation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.DeviceLocation.entity.DeviceLocation;
import ru.baikalsr.backend.DeviceLocation.repository.DeviceLocationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceLocationService {

    private final DeviceLocationRepository deviceLocationRepository;

    public List<DeviceLocation> getByPeriod(UUID deviceId, Instant from, Instant to) {
        return deviceLocationRepository.findByDevice_IdAndTsBetweenOrderByTs(deviceId, from, to);
    }

    public Page<DeviceLocation> getPage(UUID deviceId, Pageable pageable) {
        return deviceLocationRepository.findByDevice_IdOrderByTsDesc(deviceId, pageable);
    }

    public Optional<DeviceLocation> getLastAt(UUID deviceId, Instant at) {
        return deviceLocationRepository.findFirstByDevice_IdAndTsLessThanEqualOrderByTsDesc(deviceId, at);
    }

    @Transactional
    public DeviceLocation append(Device device,
                                 Instant ts,
                                 Double lat,
                                 Double lon,
                                 Double accuracy,
                                 Double altitude,
                                 Double speed,
                                 Double heading,
                                 String source,
                                 Boolean mock) {
        var location = DeviceLocation.builder()
                .device(device)
                .ts(ts)
                .lat(lat)
                .lon(lon)
                .accuracyM(accuracy)
                .altitudeM(altitude)
                .speedMps(speed)
                .headingDeg(heading)
                .source(source)
                .mock(mock)
                .receivedAt(Instant.now())
                .build();
        return deviceLocationRepository.save(location);
    }

    @Transactional
    public void purgeDevice(UUID deviceId) {
        deviceLocationRepository.deleteByDevice_Id(deviceId);
    }
}
