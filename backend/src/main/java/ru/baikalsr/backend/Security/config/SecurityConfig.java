package ru.baikalsr.backend.Security.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.DispatcherServlet;
import ru.baikalsr.backend.Security.DynamicLdapProvider;
import ru.baikalsr.backend.Security.JwtAuthFilter;
import ru.baikalsr.backend.User.service.AppUserDetailsService;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final DynamicLdapProvider dynamicLdapProvider; // твой уже созданный
    private final AppUserDetailsService uds;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;

    @Bean
    AuthenticationProvider localAuthProvider(UserDetailsService uds, PasswordEncoder enc) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication a) throws AuthenticationException {
                if (!(a instanceof UsernamePasswordAuthenticationToken)) return null;
                String username = a.getName();
                String raw = (String) a.getCredentials();

                var u = uds.loadUserByUsername(username);           // UsernameNotFoundException → цепочка пойдёт дальше
                if (!enc.matches(raw, u.getPassword())) {
                    throw new BadCredentialsException("Bad credentials");
                }
                return new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
            }
            @Override
            public boolean supports(Class<?> c) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(c);
            }
        };
    }

//    @Bean
//    AuthenticationManager authenticationManager(UserDetailsService uds,
//                                                PasswordEncoder encoder,
//                                                AuthenticationProvider dynamicLdapProvider) {
//        var dao = new DaoAuthenticationProvider();           // да, конструктор помечен deprecated, но работает
//        dao.setUserDetailsService(uds);
//        dao.setPasswordEncoder(encoder);
//        return new ProviderManager(List.of(dao, dynamicLdapProvider));
//    }

    @Bean
    AuthenticationManager authenticationManager(HttpSecurity http,
                                                PasswordEncoder encoder) throws Exception {
        var builder = http.getSharedObject(AuthenticationManagerBuilder.class);

        // DAO (UserDetailsService + PasswordEncoder)
        //builder.userDetailsService(uds).passwordEncoder(encoder);
        builder.authenticationProvider(localAuthProvider(uds, encoder));

        // Плюс кастомный LDAP-провайдер в ту же цепочку
        builder.authenticationProvider(dynamicLdapProvider);

        return builder.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain filterChain(HttpSecurity http,
                                    AuthenticationManager am) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/*/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/health_check").permitAll()
                        .requestMatchers("/api/v1/devices/register").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationManager(am)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain uiChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}
