package edu.udea.hidrologia.shared.storage;

public interface ImageStorageService {

    StoredImage upload(ImageUpload image);

    void delete(String publicId);
}
