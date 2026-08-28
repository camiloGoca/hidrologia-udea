package edu.udea.hidrologia.shared.turnstile;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "hidrologia.turnstile")
public class TurnstileProperties {

    private boolean enabled;
    private String secretKey;
    private String expectedHostnames;
    private String expectedAction;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(3);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = normalizeConfigValue(secretKey);
    }

    public String getExpectedHostnames() {
        return expectedHostnames;
    }

    public void setExpectedHostnames(String expectedHostnames) {
        this.expectedHostnames = normalizeConfigValue(expectedHostnames);
    }

    public String getExpectedAction() {
        return expectedAction;
    }

    public void setExpectedAction(String expectedAction) {
        this.expectedAction = normalizeConfigValue(expectedAction);
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    void validateRequiredConfiguration() {
        if (enabled && !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "Turnstile secretKey is required when hidrologia.turnstile.enabled=true");
        }
    }

    Set<String> expectedHostnameSet() {
        if (!StringUtils.hasText(expectedHostnames)) {
            return Set.of();
        }

        return Arrays.stream(expectedHostnames.split(","))
                .map(String::trim)
                .map(TurnstileProperties::stripWrappingQuotes)
                .map(hostname -> hostname.toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeConfigValue(String value) {
        if (value == null) {
            return null;
        }

        return stripWrappingQuotes(value.trim()).trim();
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}
