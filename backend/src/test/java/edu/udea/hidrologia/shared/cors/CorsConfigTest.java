package edu.udea.hidrologia.shared.cors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    @Test
    void previewOriginsRemainClosedWhenPreviewPatternIsNotConfigured() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(
                java.util.List.of("https://hidrologia-udea.web.app", "https://hidrologia-udea.firebaseapp.com"));

        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/links");
        request.addHeader(HttpHeaders.ORIGIN, "https://hidrologia-udea--pr-12.web.app");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://hidrologia-udea--pr-12.web.app")).isNull();
    }
}
