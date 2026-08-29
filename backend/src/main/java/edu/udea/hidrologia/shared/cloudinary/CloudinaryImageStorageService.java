package edu.udea.hidrologia.shared.cloudinary;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageRequest;
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
        return upload(image, new ImageStorageRequest(QUESTIONS_FOLDER, "question"));
    }

    @Override
    public StoredImage upload(ImageUpload image, ImageStorageRequest request) {
        String publicId = request.publicIdPrefix() + "-" + UUID.randomUUID();

        try {
            Map<?, ?> result = cloudinary.uploader().upload(image.content(), ObjectUtils.asMap(
                    "folder", request.folder(),
                    "public_id", publicId,
                    "overwrite", false,
                    "resource_type", "image",
                    "transformation", new Transformation<>().width(MAX_STORED_DIMENSION)
                            .height(MAX_STORED_DIMENSION)
                            .crop("limit")));

            return new StoredImage(
                    stringValue(result, "public_id"),
                    stringValue(result, "secure_url"),
                    imageFormat(result),
                    intValue(result, "width"),
                    intValue(result, "height"),
                    longValue(result, "bytes"));
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException("Image storage is temporarily unavailable", exception);
        }
    }

    @Override
    public ImageDeletionResult delete(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader()
                    .destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
            String deletionResult = stringValue(result, "result");
            if ("ok".equals(deletionResult)) {
                return ImageDeletionResult.DELETED;
            }
            if ("not found".equals(deletionResult)) {
                return ImageDeletionResult.NOT_FOUND;
            }

            throw new ImageStorageException(
                    "Image storage deletion could not be verified",
                    new IllegalStateException("Unexpected Cloudinary deletion result"));
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException("Image storage is temporarily unavailable", exception);
        }
    }

    private String stringValue(Map<?, ?> result, String key) {
        Object value = result.get(key);

        return value == null ? "" : value.toString();
    }

    private String imageFormat(Map<?, ?> result) {
        String format = stringValue(result, "format").toLowerCase(Locale.ROOT);
        if ("jpeg".equals(format)) {
            return "jpg";
        }
        if ("jpg".equals(format) || "png".equals(format)) {
            return format;
        }

        throw new ImageStorageException(
                "Image storage returned an unsupported image format",
                new IllegalStateException("Unsupported image format"));
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
