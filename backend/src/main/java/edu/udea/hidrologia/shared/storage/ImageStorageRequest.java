package edu.udea.hidrologia.shared.storage;

public record ImageStorageRequest(String folder, String publicIdPrefix) {

    public ImageStorageRequest {
        if (folder == null || folder.isBlank()) {
            throw new IllegalArgumentException("Image storage folder is required");
        }
        if (publicIdPrefix == null || publicIdPrefix.isBlank()) {
            throw new IllegalArgumentException("Image storage public id prefix is required");
        }
    }
}
