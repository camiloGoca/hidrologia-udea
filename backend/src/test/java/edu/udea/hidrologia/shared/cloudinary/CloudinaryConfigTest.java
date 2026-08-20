package edu.udea.hidrologia.shared.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CloudinaryConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CloudinaryConfig.class);

    @Test
    void bindsCloudinaryPropertiesWithoutCreatingBeanWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.cloudinary.enabled=false",
                        "hidrologia.cloudinary.cloud-name=test-cloud",
                        "hidrologia.cloudinary.api-key=test-api-key",
                        "hidrologia.cloudinary.api-secret=test-api-secret")
                .run(context -> {
                    CloudinaryProperties properties = context.getBean(CloudinaryProperties.class);

                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.getCloudName()).isEqualTo("test-cloud");
                    assertThat(properties.getApiKey()).isEqualTo("test-api-key");
                    assertThat(properties.getApiSecret()).isEqualTo("test-api-secret");
                    assertThat(context).doesNotHaveBean(Cloudinary.class);
                });
    }

    @Test
    void createsCloudinaryBeanWithFictitiousCredentialsWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.cloudinary.enabled=true",
                        "hidrologia.cloudinary.cloud-name=test-cloud",
                        "hidrologia.cloudinary.api-key=test-api-key",
                        "hidrologia.cloudinary.api-secret=test-api-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(CloudinaryProperties.class);
                    assertThat(context).hasSingleBean(Cloudinary.class);
                });
    }

    @Test
    void failsClearlyWhenEnabledWithoutCredentials() {
        contextRunner
                .withPropertyValues("hidrologia.cloudinary.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining(
                                    "Cloudinary credentials are required when hidrologia.cloudinary.enabled=true");
                });
    }
}
