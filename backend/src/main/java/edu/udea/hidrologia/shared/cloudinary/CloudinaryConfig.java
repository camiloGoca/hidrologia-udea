package edu.udea.hidrologia.shared.cloudinary;

import java.util.Map;

import com.cloudinary.Cloudinary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    @ConditionalOnProperty(prefix = "hidrologia.cloudinary", name = "enabled", havingValue = "true")
    Cloudinary cloudinary(CloudinaryProperties properties) {
        properties.validateRequiredCredentials();

        return new Cloudinary(Map.of(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true));
    }
}
