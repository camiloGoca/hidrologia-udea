package edu.udea.hidrologia.shared.cors;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration productionConfiguration = new CorsConfiguration();
        productionConfiguration.setAllowedOrigins(properties.getAllowedOrigins());
        productionConfiguration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        productionConfiguration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT));
        productionConfiguration.setAllowCredentials(false);

        CorsConfiguration previewConfiguration = new CorsConfiguration();
        if (properties.hasPreviewOriginPattern()) {
            previewConfiguration.setAllowedOriginPatterns(List.of(properties.getPreviewOriginPattern()));
        }
        previewConfiguration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.HEAD.name(),
                HttpMethod.OPTIONS.name()));
        previewConfiguration.setAllowedHeaders(List.of(HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE));
        previewConfiguration.setAllowCredentials(false);

        return request -> {
            if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                return null;
            }

            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (properties.isPreviewOrigin(origin)) {
                return previewConfiguration;
            }

            return productionConfiguration;
        };
    }
}
