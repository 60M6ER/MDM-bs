package ru.baikalsr.backend.Device.enums;

public enum DeviceEvents {
    /** Устройство подключено и онлайн */
    ONLINE,
    /** Устройство отключено и офлайн */
    OFFLINE,
    /** Устройство аварийно завершило работу */
    CRASH,
    /** Устройство физически упало на пол */
    FALL_DETECTED,
    /** Приложение на устройстве обновлено */
    APP_UPDATED,
    BOOT_COMPLETED,
    APP_CRASHED,
    APP_STARTED,
    APP_STOPPED,
    NETWORK_UP,
    NETWORK_DOWN,
}
