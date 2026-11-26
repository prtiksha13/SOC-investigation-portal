package com.socsentinel.portal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuditAuthenticationSuccessHandler successHandler,
                                           AuditAuthenticationFailureHandler failureHandler,
                                           AuditLogoutHandler logoutHandler) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/login", "/forgot-password", "/forgot-password/**", "/favicon.ico", "/styles.css", "/soc-theme.css", "/login.css", "/app.js", "/error").permitAll()
                .requestMatchers("/api/user").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/alerts").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/alerts/cleanup-demo-data").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/alerts/**", "/api/notes/**", "/api/metrics", "/api/logs/uploads")
                                                    .hasAnyRole("ANALYST_L1", "ANALYST_L2", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/notes")
                                                    .hasAnyRole("ANALYST_L1", "ANALYST_L2", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/alerts/*/status")
                                                    .hasAnyRole("ANALYST_L1", "ANALYST_L2", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/alerts/*/escalate")
                                                    .hasRole("ANALYST_L1")
                                                .requestMatchers(HttpMethod.POST, "/api/alerts/*/investigations")
                                                    .hasAnyRole("ANALYST_L1", "ANALYST_L2", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/alerts/*/severity")
                                                    .hasAnyRole("ANALYST_L2", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/alerts", "/api/logs/upload", "/api/alerts/*/ai-analysis")
                                                    .hasAnyRole("ANALYST_L2", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login").permitAll()
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            )
            .logout(logout -> logout
                .addLogoutHandler(logoutHandler)
                .permitAll())
            .exceptionHandling(exception -> exception.accessDeniedHandler((request, response, accessDeniedException) -> {
                                                if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("text/plain");
                    response.getWriter().write("Access Denied");
                    return;
                }
                new AccessDeniedHandlerImpl().handle(request, response, accessDeniedException);
            }))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
