package edu.udea.hidrologia.shared.cloudinary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "hidrologia.cloudinary")
public class CloudinaryProperties {

    private boolean enabled;
    private String cloudName;
    private String apiKey;
    private String apiSecret;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    void validateRequiredCredentials() {
        if (!StringUtils.hasText(cloudName)
                || !StringUtils.hasText(apiKey)
                || !StringUtils.hasText(apiSecret)) {
            throw new IllegalStateException(
                    "Cloudinary credentials are required when hidrologia.cloudinary.enabled=true");
        }
    }
}
