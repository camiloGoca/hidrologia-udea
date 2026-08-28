package edu.udea.hidrologia.shared.cors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "hidrologia.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null) {
            this.allowedOrigins = List.of();
            return;
        }

        List<String> normalizedOrigins = new ArrayList<>();
        for (String origin : allowedOrigins) {
            String normalizedOrigin = normalizeOrigin(origin);
            if (StringUtils.hasText(normalizedOrigin) && !normalizedOrigins.contains(normalizedOrigin)) {
                normalizedOrigins.add(normalizedOrigin);
            }
        }

        this.allowedOrigins = List.copyOf(normalizedOrigins);
    }

    private String normalizeOrigin(String origin) {
        if (origin == null) {
            return null;
        }

        return stripWrappingQuotes(origin.trim()).trim();
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}
