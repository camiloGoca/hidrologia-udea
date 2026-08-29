package edu.udea.hidrologia.shared.cors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(prefix = "hidrologia.cors")
public class CorsProperties {

    private static final Pattern HIDROLOGIA_FIREBASE_PREVIEW_ORIGIN =
            Pattern.compile("^https://hidrologia-udea--[a-z0-9-]+\\.web\\.app$", Pattern.CASE_INSENSITIVE);

    private List<String> allowedOrigins = List.of();
    private String previewOriginPattern;

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

    public String getPreviewOriginPattern() {
        return previewOriginPattern;
    }

    public void setPreviewOriginPattern(String previewOriginPattern) {
        this.previewOriginPattern = normalizeOrigin(previewOriginPattern);
    }

    public boolean hasPreviewOriginPattern() {
        return StringUtils.hasText(previewOriginPattern);
    }

    public boolean isPreviewOrigin(String origin) {
        String normalizedOrigin = normalizeOrigin(origin);
        if (!StringUtils.hasText(normalizedOrigin)
                || !hasPreviewOriginPattern()
                || !HIDROLOGIA_FIREBASE_PREVIEW_ORIGIN.matcher(normalizedOrigin).matches()) {
            return false;
        }

        CorsConfiguration previewConfiguration = new CorsConfiguration();
        previewConfiguration.setAllowedOriginPatterns(List.of(previewOriginPattern));

        return previewConfiguration.checkOrigin(normalizedOrigin) != null;
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
