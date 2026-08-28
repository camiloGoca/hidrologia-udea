package edu.udea.hidrologia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProductionProfileConfigurationTest {

    @Test
    void productionProfileReactivatesPersistenceAndHardensPublicDiagnostics() throws IOException {
        PropertySource<?> properties = load("application-prod.yml");

        assertThat(properties.getProperty("spring.autoconfigure.exclude[0]"))
                .isEqualTo("org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration");
        assertThat(properties.getProperty("spring.autoconfigure.exclude[1]")).isNull();

        assertThat(properties.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(properties.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.jpa.open-in-view")).isEqualTo(false);

        assertThat(properties.getProperty("springdoc.api-docs.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(properties.getProperty("management.endpoint.health.show-details")).isEqualTo("never");

        assertThat(properties.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-binding-errors")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-exception")).isEqualTo(false);
    }

    @Test
    void baseConfigurationUsesCloudRunPortFallback() throws IOException {
        PropertySource<?> properties = load("application.yml");

        assertThat(properties.getProperty("server.port")).isEqualTo("${PORT:8080}");
    }

    private PropertySource<?> load(String resourceName) throws IOException {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName));

        assertThat(propertySources).hasSize(1);

        return propertySources.get(0);
    }
}
