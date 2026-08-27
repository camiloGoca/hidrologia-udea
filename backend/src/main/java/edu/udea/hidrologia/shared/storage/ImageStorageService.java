package edu.udea.hidrologia.shared.storage;

public interface ImageStorageService {

    StoredImage upload(ImageUpload image);

    StoredImage upload(ImageUpload image, ImageStorageRequest request);

    ImageDeletionResult delete(String publicId);
}
