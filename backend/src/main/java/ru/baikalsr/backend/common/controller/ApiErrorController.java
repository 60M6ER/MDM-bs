package ru.baikalsr.backend.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/error")
public class ApiErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public ApiErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @GetMapping
    public Object handleError(HttpServletRequest request, HttpServletResponse response) {
        WebRequest webRequest = new ServletWebRequest(request);

        var attrs = errorAttributes.getErrorAttributes(
                webRequest,
                ErrorAttributeOptions.defaults()
                .including(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.EXCEPTION)
        );

        int status = (int) attrs.getOrDefault("status", 500);
        String path = (String) attrs.getOrDefault("path", request.getRequestURI());
        String error = (String) attrs.getOrDefault("error", "Error");
        String message = (String) attrs.getOrDefault("message", "Unexpected error");

        // 1) API → всегда JSON (Problem Details)
        if (path.startsWith("/api/")) {
            var pd = ProblemDetail.forStatus(status);
            pd.setTitle(error);
            pd.setDetail(message);
            pd.setProperty("path", path);
            return pd;
        }

        // 2) НЕ API:
        //   - если просили HTML (браузерное открытие SPA-роута) — отдать index.html
        //   - иначе — тоже JSON Problem Details
        String accept = request.getHeader("Accept");
        boolean wantsHtml = accept != null && accept.contains("text/html");

        if (wantsHtml) {
            response.setStatus(HttpStatus.OK.value());
            return "forward:/index.html";
        }

        var pd = ProblemDetail.forStatus(status);
        pd.setTitle(error);
        pd.setDetail(message);
        pd.setProperty("path", path);
        return pd;
    }
}
