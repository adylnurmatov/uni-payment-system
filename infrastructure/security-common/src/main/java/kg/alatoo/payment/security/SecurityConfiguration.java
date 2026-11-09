package kg.alatoo.payment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kg.alatoo.payment.rest.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Безопасность:
 * <ul>
 *     <li>/api/admin/** — Basic Auth (логин admin, пароль ADMIN_PASSWORD);</li>
 *     <li>/admin/** — Thymeleaf-панель, form login с тем же паролем;</li>
 *     <li>/api/v1/** — мерчанты, аутентификация по X-Api-Key делается в CurrentMerchantArgumentResolver;</li>
 *     <li>/mock-abc/**, Swagger, actuator/health — открыты.</li>
 * </ul>
 */
@Configuration
public class SecurityConfiguration {
    private static final RequestMatcher API_REQUESTS =
            request -> pathOf(request).startsWith("/api/") || pathOf(request).startsWith("/mock-abc/");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                // REST API без cookie-сессий — CSRF ему не нужен; для форм /admin CSRF остаётся включённым
                .csrf(csrf -> csrf.ignoringRequestMatchers(API_REQUESTS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(jsonUnauthorized(objectMapper), API_REQUESTS));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(@Value("${admin.username:admin}") String username,
                                                 @Value("${admin.password:}") String password,
                                                 PasswordEncoder passwordEncoder) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD is not set. Put it into .env or the environment.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Для API отвечаем JSON в едином формате ошибок, а не HTML-страницей логина. */
    private static AuthenticationEntryPoint jsonUnauthorized(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("WWW-Authenticate", "Basic realm=\"admin\"");
            objectMapper.writeValue(response.getOutputStream(),
                    ErrorResponse.of("UNAUTHORIZED", "Admin credentials are required"));
        };
    }

    private static String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
