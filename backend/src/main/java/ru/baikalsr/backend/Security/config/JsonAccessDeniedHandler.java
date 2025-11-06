package ru.baikalsr.backend.Security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper om;

    public JsonAccessDeniedHandler(ObjectMapper om) {
        this.om = om;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        var pd = ProblemDetail.forStatus(403);
        pd.setTitle("Forbidden");
        pd.setDetail("Недостаточно прав");
        pd.setType(java.net.URI.create("about:blank"));
        pd.setProperty("code", "FORBIDDEN");
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());

        res.setStatus(403);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/problem+json");
        om.writeValue(res.getOutputStream(), pd);
    }
}
