package ru.baikalsr.backend.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/device_exchange")) {
            filterChain.doFilter(request, response);
            return;
        }

        String deviceIdHeader = request.getHeader("X-Device-Id");
        String secretHeader = request.getHeader("X-Device-Secret");

        if (deviceIdHeader == null || secretHeader == null) {
            filterChain.doFilter(request, response); // или сразу 401
            return;
        }

        try {
            UUID deviceId = UUID.fromString(deviceIdHeader);
            var device = deviceService.findById(deviceId)
                    .orElseThrow();

            if (!deviceService.matchesSecret(device, secretHeader)) {
                throw new IllegalArgumentException("Bad secret");
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
        } catch (Exception e) {
            // опционально: можно сразу ответить 401/403
            // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // return;
        }

        filterChain.doFilter(request, response);
    }
}
