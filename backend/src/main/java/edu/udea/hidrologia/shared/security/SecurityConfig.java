package edu.udea.hidrologia.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminBearerAuthenticationFilter adminBearerAuthenticationFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            AdminBearerAuthenticationFilter adminBearerAuthenticationFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) {
        this.adminBearerAuthenticationFilter = adminBearerAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/links").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sections").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sections/{slug}/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags/{slug}/posts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/questions").permitAll()
                        .requestMatchers(
                                "/api/v1/health",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(adminBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
