package edu.udea.hidrologia.shared.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "hidrologia.firebase")
public class FirebaseProperties {

    private boolean enabled;
    private String projectId;
    private String adminUid;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getAdminUid() {
        return adminUid;
    }

    public void setAdminUid(String adminUid) {
        this.adminUid = adminUid;
    }

    void validateRequiredConfiguration() {
        if (!StringUtils.hasText(projectId) || !StringUtils.hasText(adminUid)) {
            throw new IllegalStateException(
                    "Firebase projectId and adminUid are required when hidrologia.firebase.enabled=true");
        }
    }
}
