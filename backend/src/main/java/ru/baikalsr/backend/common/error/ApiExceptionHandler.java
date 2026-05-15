package ru.baikalsr.backend.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        String code = ex.getReason();
        String fallbackDetail = ex.getBody() != null ? ex.getBody().getDetail() : null;
        String detail = ApiErrorMessages.resolve(code, fallbackDetail != null ? fallbackDetail : "Произошла ошибка.");

        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(status.toString());
        pd.setDetail(detail);
        pd.setProperty("code", code);
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }
}
