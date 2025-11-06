package ru.baikalsr.backend.Security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper om;

    public JsonAuthEntryPoint(ObjectMapper om) { // Jackson из контекста
        this.om = om;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        var pd = ProblemDetail.forStatus(401);
        pd.setTitle("Unauthorized");
        pd.setDetail("Требуется аутентификация");
        pd.setType(java.net.URI.create("about:blank"));
        pd.setProperty("code", "UNAUTHORIZED");
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());

        res.setStatus(401);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/problem+json");
        om.writeValue(res.getOutputStream(), pd);
    }
}
