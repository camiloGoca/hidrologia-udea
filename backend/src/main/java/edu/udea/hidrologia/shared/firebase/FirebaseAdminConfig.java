package edu.udea.hidrologia.shared.firebase;

import java.io.IOException;
import java.util.Optional;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseAdminConfig {

    @Bean
    @ConditionalOnMissingBean
    FirebaseCredentialsProvider firebaseCredentialsProvider() {
        return GoogleCredentials::getApplicationDefault;
    }

    @Bean
    @ConditionalOnProperty(prefix = "hidrologia.firebase", name = "enabled", havingValue = "true")
    FirebaseApp firebaseApp(FirebaseProperties properties, FirebaseCredentialsProvider credentialsProvider) {
        properties.validateRequiredConfiguration();

        GoogleCredentials credentials;
        try {
            credentials = credentialsProvider.getApplicationDefault();
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Application Default Credentials could not be loaded", exception);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(properties.getProjectId())
                .build();

        return getDefaultFirebaseApp().orElseGet(() -> FirebaseApp.initializeApp(options));
    }

    @Bean
    @ConditionalOnProperty(prefix = "hidrologia.firebase", name = "enabled", havingValue = "true")
    FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private Optional<FirebaseApp> getDefaultFirebaseApp() {
        return FirebaseApp.getApps().stream()
                .filter(app -> FirebaseApp.DEFAULT_APP_NAME.equals(app.getName()))
                .findFirst();
    }
}
