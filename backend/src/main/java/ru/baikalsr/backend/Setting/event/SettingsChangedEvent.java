package ru.baikalsr.backend.Setting.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.baikalsr.backend.Setting.enums.SettingGroup;

@Getter
public class SettingsChangedEvent extends ApplicationEvent {

    private final SettingGroup settingGroup;

    public SettingsChangedEvent(Object source, SettingGroup settingGroup) {
        super(source);
        this.settingGroup = settingGroup;
    }


}
