package com.breathingroom.backend.security;
import org.springframework.beans.factory.annotation.Qualifier;
import com.breathingroom.backend.logic.LoginThrottle;
import com.breathingroom.backend.logic.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

/**
 * The rules of the front door:
 *  - only /api/auth/register, /api/auth/login and /api/health are public,
 *  - everything else needs a valid token,
 *  - no cookies or server sessions, so CSRF does not apply (the token travels in a header).
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Stops Spring from creating a hidden default user with a random password. Real sign-in goes through /api/auth/login only. */
    @Bean
    public UserDetailsService noDefaultUser() {
        return username -> {
            throw new UsernameNotFoundException("No such user");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TokenService tokenService(@Value("${app.security.jwt-secret:}") String secret,
                                     @Value("${app.security.token-hours:8}") long hours) {
        byte[] key;
        if (secret == null || secret.isBlank()) {
            key = new byte[48];
            new SecureRandom().nextBytes(key);
            log.warn("JWT_SECRET is not set, so a random secret is used for this run. Everyone is signed out when the backend restarts. "
                    + "Set JWT_SECRET (32+ characters) in backend/.env to keep people signed in.");
        } else {
            key = secret.getBytes(StandardCharsets.UTF_8);
            if (key.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 characters. Make one with: openssl rand -base64 48");
            }
        }
        return new TokenService(key, Duration.ofHours(hours), Clock.systemUTC());
    }

    @Bean
    public LoginThrottle loginThrottle() {
        return new LoginThrottle(5, 10 * 60_000L, 10_000, System::currentTimeMillis);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenService tokens,
                                           @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(cors))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new TokenAuthFilter(tokens), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
