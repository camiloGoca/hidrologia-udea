package edu.udea.hidrologia.shared.turnstile;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TurnstileProperties.class)
public class TurnstileConfig {

    private static final String TURNSTILE_BASE_URL = "https://challenges.cloudflare.com/turnstile/v0";

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    TurnstileVerifier turnstileVerifier(TurnstileProperties properties, RestClient.Builder restClientBuilder) {
        properties.validateRequiredConfiguration();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        RestClient restClient = restClientBuilder
                .baseUrl(TURNSTILE_BASE_URL)
                .requestFactory(requestFactory)
                .build();

        return new CloudflareTurnstileVerifier(properties, restClient);
    }
}
