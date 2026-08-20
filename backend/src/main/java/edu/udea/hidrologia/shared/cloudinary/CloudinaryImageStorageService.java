package edu.udea.hidrologia.shared.cloudinary;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageUpload;
import edu.udea.hidrologia.shared.storage.StoredImage;

@Service
@ConditionalOnBean(Cloudinary.class)
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final String QUESTIONS_FOLDER = "hidrologia-udea/questions";
    private static final int MAX_STORED_DIMENSION = 2400;

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public StoredImage upload(ImageUpload image) {
        String publicId = "question-" + UUID.randomUUID();

        try {
            Map<?, ?> result = cloudinary.uploader().upload(image.content(), ObjectUtils.asMap(
                    "folder", QUESTIONS_FOLDER,
                    "public_id", publicId,
                    "overwrite", false,
                    "resource_type", "image",
                    "transformation", new Transformation<>().width(MAX_STORED_DIMENSION)
                            .height(MAX_STORED_DIMENSION)
                            .crop("limit")));

            return new StoredImage(
                    stringValue(result, "public_id"),
                    stringValue(result, "secure_url"),
                    stringValue(result, "format"),
                    intValue(result, "width"),
                    intValue(result, "height"),
                    longValue(result, "bytes"));
        } catch (IOException exception) {
            throw new ImageStorageException("Image storage is temporarily unavailable", exception);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException exception) {
            throw new ImageStorageException("Image storage is temporarily unavailable", exception);
        }
    }

    private String stringValue(Map<?, ?> result, String key) {
        Object value = result.get(key);

        return value == null ? "" : value.toString();
    }

    private int intValue(Map<?, ?> result, String key) {
        Object value = result.get(key);

        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    private long longValue(Map<?, ?> result, String key) {
        Object value = result.get(key);

        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }
}
