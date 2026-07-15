package com.mdeg.docsportal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ProjectAuthenticationSuccessHandler projectAuthenticationSuccessHandler;

    // Admin credentials
    @Value("${docs-portal.auth.admin.username}")
    private String adminUsername;

    @Value("${docs-portal.auth.admin.password}")
    private String adminPassword;

    // Viewer credentials
    @Value("${docs-portal.auth.viewer.username}")
    private String viewerUsername;

    @Value("${docs-portal.auth.viewer.password}")
    private String viewerPassword;

    @Value("${docs-portal.webhook.secret}")
    private String webhookSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/webhook/gitea")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**").permitAll()
                // Webhook authenticity is enforced by its HMAC signature, not a browser session.
                .requestMatchers("/api/webhook/gitea").permitAll()
                // Admin-only endpoints
                .requestMatchers("/admin/**", "/api/repositories/**", "/api/mkdocs-config/**",
                                 "/api/build/trigger", "/api/webhook/config", "/api/projects/**").hasRole("ADMIN")
                // Viewer + Admin: docs, health, status, site content
                .requestMatchers("/site/**", "/api/health", "/api/build/status",
                                 "/api/doc-pages", "/docs").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(projectAuthenticationSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    request.setAttribute("login-failed", true);
                    response.sendRedirect(request.getContextPath() + "/login?error");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )
            .sessionManagement(session -> session
                .maximumSessions(-1)
            )
            .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            // Admin can view docs AND manage the system
            User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN", "VIEWER")
                .build(),
            // Viewer can only read documentation and check status
            User.builder()
                .username(viewerUsername)
                .password(passwordEncoder().encode(viewerPassword))
                .roles("VIEWER")
                .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public String webhookSecretBean() {
        return webhookSecret;
    }
}
