package ru.baikalsr.backend.Security.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.admin")
@Data
public class AdminProperty {
    private String username;
    private String password;
}
