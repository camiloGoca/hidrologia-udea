package edu.udea.hidrologia.shared.turnstile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class TurnstileConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TurnstileConfig.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void bindsTurnstileProperties() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.turnstile.enabled=true",
                        "hidrologia.turnstile.secret-key=test-secret",
                        "hidrologia.turnstile.expected-hostnames=localhost,hidrologia.example.edu",
                        "hidrologia.turnstile.expected-action=test")
                .run(context -> {
                    TurnstileProperties properties = context.getBean(TurnstileProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getSecretKey()).isEqualTo("test-secret");
                    assertThat(properties.expectedHostnameSet())
                            .containsExactly("localhost", "hidrologia.example.edu");
                    assertThat(properties.getExpectedAction()).isEqualTo("test");
                    assertThat(context).hasSingleBean(TurnstileVerifier.class);
                });
    }

    @Test
    void createsVerifierWithoutSecretWhenDisabled() {
        contextRunner
                .withPropertyValues("hidrologia.turnstile.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(TurnstileVerifier.class));
    }

    @Test
    void failsClearlyWithoutSecretWhenEnabled() {
        contextRunner
                .withPropertyValues("hidrologia.turnstile.enabled=true")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining(
                                "Turnstile secretKey is required when hidrologia.turnstile.enabled=true"));
    }

    @Test
    void allowsBlankExpectedActionWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.turnstile.enabled=true",
                        "hidrologia.turnstile.secret-key=test-secret",
                        "hidrologia.turnstile.expected-action=")
                .run(context -> {
                    assertThat(context).hasSingleBean(TurnstileVerifier.class);
                    assertThat(context.getBean(TurnstileProperties.class).getExpectedAction()).isEmpty();
                });
    }
}
