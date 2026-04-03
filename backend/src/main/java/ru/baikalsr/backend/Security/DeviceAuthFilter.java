package ru.baikalsr.backend.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.baikalsr.backend.Device.service.DeviceService;
import ru.baikalsr.backend.Security.model.DevicePrincipal;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceAuthFilter extends OncePerRequestFilter {

    private final DeviceService deviceService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String deviceIdHeader = request.getHeader("X-Device-Id");
        String secretHeader = request.getHeader("X-Device-Secret");

        if (deviceIdHeader == null || secretHeader == null) {
            commenceUnauthorized(request, response, "DEVICE_AUTH_HEADERS_MISSING");
            return;
        }

        try {
            UUID deviceId = UUID.fromString(deviceIdHeader);
            var device = deviceService.findById(deviceId)
                    .orElseThrow(() -> new BadCredentialsException("DEVICE_NOT_FOUND"));

            if (!deviceService.matchesSecret(device, secretHeader)) {
                throw new BadCredentialsException("DEVICE_SECRET_INVALID");
            }

            DevicePrincipal principal = new DevicePrincipal(
                    device.getId().toString()
            );

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            commenceUnauthorized(request, response, e.getMessage());
        } catch (IllegalArgumentException e) {
            commenceUnauthorized(request, response, "DEVICE_ID_INVALID");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/device_exchange");
    }

    private void commenceUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(message)
        );
    }
}
