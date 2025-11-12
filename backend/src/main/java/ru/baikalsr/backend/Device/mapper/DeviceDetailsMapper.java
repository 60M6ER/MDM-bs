package ru.baikalsr.backend.Device.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import ru.baikalsr.backend.Device.dto.DeviceDetailsDto;
import ru.baikalsr.backend.Device.dto.DeviceListItemDto;
import ru.baikalsr.backend.Device.entity.Device;
import ru.baikalsr.backend.Device.entity.DeviceDetailsCurrent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ручной маппер без MapStruct.
 */
@Component
public class DeviceDetailsMapper {

    /** Полный детальный вывод из read‑модели device_details_current. */
    public DeviceDetailsDto toDetails(DeviceDetailsCurrent src) {
        if (src == null) return null;
        return new DeviceDetailsDto(
                // Identity
                src.getDeviceId(),
                src.getDeviceName(),
                src.getSerialNumber(),
                src.getInventoryNumber(),
                src.getStatus(),
                src.getModel(),
                src.getManufacturer(),

                // Lifecycle
                src.getEnrolledAt(),
                src.getDeactivatedAt(),
                src.getDeviceCreatedAt(),
                src.getDeviceUpdatedAt(),

                // State
                src.getStateLastSeenAt(),
                src.getStateUpdatedAt(),
                src.getOnline(),
                src.getCharging(),
                src.getBatteryLevel(),
                src.getOsVersion(),
                src.getAppVersion(),
                src.getNetworkType(),
                src.getWifiSsid(),
                src.getIpAddress(),
                src.getStorageTotalMb(),
                src.getStorageFreeMb(),
                src.getCpuTempC(),

                // Owner
                src.getOwnerDisplay(),
                src.getOwnerUserId(),
                src.getOwnerAssignedAt(),

                // Location
                src.getLat(),
                src.getLon(),
                src.getAccuracyM(),
                src.getAltitudeM(),
                src.getSpeedMps(),
                src.getHeadingDeg(),
                src.getLocationSource(),
                src.getLocationIsMock(),
                src.getLocationTs(),
                src.getLocationReceivedAt(),

                // Department
                src.getDepartmentId(),
                src.getDepartmentAssignedAt()
        );
    }

    /** Краткий вывод для списка устройств. */
    public DeviceListItemDto toListItem(Device src) {
        if (src == null) return null;
        return new DeviceListItemDto(
                src.getId(),
                src.getInventoryNumber(),
                src.getModel(),
                src.getManufacturer(),
                src.getDeviceName(),
                src.getSerialNumber()
        );
    }

    /** Helpers для коллекций/страниц. */
    public List<DeviceListItemDto> toListItems(List<Device> list) {
        return list.stream().map(this::toListItem).collect(Collectors.toList());
    }

    public Page<DeviceListItemDto> toListItems(Page<Device> page) {
        return new PageImpl<>(
                toListItems(page.getContent()),
                page.getPageable(),
                page.getTotalElements()
        );
    }
}
