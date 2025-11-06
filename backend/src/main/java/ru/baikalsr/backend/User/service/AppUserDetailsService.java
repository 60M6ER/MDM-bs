package ru.baikalsr.backend.User.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.baikalsr.backend.User.entity.User;
import ru.baikalsr.backend.User.entity.Role;
import ru.baikalsr.backend.Security.property.AdminProperty;
import ru.baikalsr.backend.User.repository.UserRepository;

import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final AdminProperty adminProperty;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var u = userRepository.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var authorities = u.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(), u.isEnabled(),
                true, true, true, authorities
        );
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    /**
     * Если пользователя нет — создать с ролью ROLE_USER и вернуть true.
     * Если уже существует — вернуть false.
     * <p>
     * Используется после успешной внешней (LDAP/AD) аутентификации,
     * чтобы локально фиксировать пользователя и права.
     */
    @Transactional
    public boolean ensureExistsWithDefaultRole(String username, String displayName) {
        final String u = normalize(username);
        var existing = userRepository.findByUsername(u);
        if (existing.isPresent()) {
            return false;
        }

        // Пароль нам для внешних логинов не нужен, но поле не должно быть null → кладём случайную заглушку
        String randomPassword = java.util.UUID.randomUUID().toString();

        User user = new User();
        user.setUsername(u);
        user.setName((displayName == null || displayName.isBlank()) ? u : displayName);
        user.setEnabled(true);
        user.setRoles(Set.of(Role.ROLE_USER));
        user.setPassword(passwordEncoder.encode(randomPassword));

        try {
            userRepository.save(user);
            log.info("Local user '{}' created with default ROLE_USER", u);
            return true;
        } catch (DataIntegrityViolationException e) {
            // На случай гонки: кто-то успел создать запись параллельно
            log.warn("Race creating user '{}': {}", u, e.getMessage());
            return false;
        }
    }

    /**
     * Загрузить локальные authorities пользователя из нашей БД.
     * Бросает UsernameNotFoundException, если пользователя нет.
     */
    @Transactional(readOnly = true)
    public Collection<SimpleGrantedAuthority> loadAuthorities(String username) {
        var u = userRepository.findByUsername(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return u.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Transactional
    public void createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            userRepository.save(user);
            log.info("{} user has been created", user.getUsername());
        } catch (DataIntegrityViolationException ignored) {

        }
    }

    @PostConstruct
    @Transactional
    protected void adminChecker() {
        if (userRepository.findByUsername(adminProperty.getUsername().toLowerCase()).isEmpty()) {
            if (Strings.isBlank(adminProperty.getPassword())) {
                throw new IllegalStateException("Admin password is empty");
            }
            User user = new User();
            user.setUsername(adminProperty.getUsername().toLowerCase());
            user.setPassword(passwordEncoder.encode(adminProperty.getPassword()));
            user.setName(adminProperty.getUsername());
            user.setEnabled(true);
            user.setRoles(Set.of(Role.ROLE_ADMINISTRATOR,  Role.ROLE_USER));
            try {
                userRepository.save(user);
                log.info("Admin user has been created");
            }
            catch (DataIntegrityViolationException ignored) { /* уже создан */ }
        }
    }
}
