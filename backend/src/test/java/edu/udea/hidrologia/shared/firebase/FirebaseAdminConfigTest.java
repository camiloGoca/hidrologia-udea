package edu.udea.hidrologia.shared.firebase;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FirebaseAdminConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseAdminConfig.class);

    @BeforeEach
    void setUp() {
        deleteFirebaseApps();
    }

    @AfterEach
    void tearDown() {
        deleteFirebaseApps();
    }

    @Test
    void bindsFirebaseProperties() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.firebase.enabled=true",
                        "hidrologia.firebase.project-id=test-project",
                        "hidrologia.firebase.admin-uid=test-admin-uid")
                .withBean(FirebaseCredentialsProvider.class, () -> this::testCredentials)
                .run(context -> {
                    FirebaseProperties properties = context.getBean(FirebaseProperties.class);

                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getProjectId()).isEqualTo("test-project");
                    assertThat(properties.getAdminUid()).isEqualTo("test-admin-uid");
                });
    }

    @Test
    void doesNotCreateFirebaseBeansWhenFirebaseIsDisabled() {
        contextRunner
                .withPropertyValues("hidrologia.firebase.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FirebaseApp.class);
                    assertThat(context).doesNotHaveBean(FirebaseAuth.class);
                });
    }

    @Test
    void createsFirebaseAppAndAuthWhenFirebaseIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.firebase.enabled=true",
                        "hidrologia.firebase.project-id=test-project",
                        "hidrologia.firebase.admin-uid=test-admin-uid")
                .withBean(FirebaseCredentialsProvider.class, () -> this::testCredentials)
                .run(context -> {
                    assertThat(context).hasSingleBean(FirebaseApp.class);
                    assertThat(context).hasSingleBean(FirebaseAuth.class);

                    FirebaseApp firebaseApp = context.getBean(FirebaseApp.class);
                    assertThat(firebaseApp.getOptions().getProjectId()).isEqualTo("test-project");
                });
    }

    @Test
    void failsClearlyWhenProjectIdIsMissingAndFirebaseIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.firebase.enabled=true",
                        "hidrologia.firebase.admin-uid=test-admin-uid")
                .withBean(FirebaseCredentialsProvider.class, () -> this::testCredentials)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining(
                                "Firebase projectId and adminUid are required when hidrologia.firebase.enabled=true"));
    }

    @Test
    void failsClearlyWhenAdminUidIsMissingAndFirebaseIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.firebase.enabled=true",
                        "hidrologia.firebase.project-id=test-project")
                .withBean(FirebaseCredentialsProvider.class, () -> this::testCredentials)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining(
                                "Firebase projectId and adminUid are required when hidrologia.firebase.enabled=true"));
    }

    @Test
    void failsClearlyWhenApplicationDefaultCredentialsCannotBeLoaded() {
        contextRunner
                .withPropertyValues(
                        "hidrologia.firebase.enabled=true",
                        "hidrologia.firebase.project-id=test-project",
                        "hidrologia.firebase.admin-uid=test-admin-uid")
                .withBean(FirebaseCredentialsProvider.class, () -> () -> {
                    throw new IOException("test adc failure");
                })
                .run(context -> assertThat(context.getStartupFailure())
                        .hasMessageContaining("Firebase Application Default Credentials could not be loaded"));
    }

    private GoogleCredentials testCredentials() {
        AccessToken token = new AccessToken("test-access-token", new Date(System.currentTimeMillis() + 3_600_000));

        return GoogleCredentials.create(token);
    }

    private void deleteFirebaseApps() {
        new ArrayList<>(FirebaseApp.getApps()).forEach(FirebaseApp::delete);
    }
}
