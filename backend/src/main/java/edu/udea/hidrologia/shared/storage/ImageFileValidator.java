package edu.udea.hidrologia.shared.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

    private static final String JPEG_FORMAT = "jpg";
    private static final String PNG_FORMAT = "png";

    private final ImageUploadProperties properties;

    public ImageFileValidator(ImageUploadProperties properties) {
        this.properties = properties;
    }

    public Optional<ImageUpload> validateOptional(MultipartFile file) {
        if (file == null) {
            return Optional.empty();
        }

        if (file.isEmpty()) {
            throw new InvalidImageException("The uploaded image is empty");
        }

        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new ImageTooLargeException("The uploaded image exceeds the maximum size");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidImageException("The uploaded image could not be read");
        }

        if (content.length == 0) {
            throw new InvalidImageException("The uploaded image is empty");
        }

        if (content.length > properties.getMaxSizeBytes()) {
            throw new ImageTooLargeException("The uploaded image exceeds the maximum size");
        }

        return Optional.of(validateImageContent(content));
    }

    private ImageUpload validateImageContent(byte[] content) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (imageInputStream == null) {
                throw new InvalidImageException("The uploaded file is not a valid JPEG or PNG image");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new InvalidImageException("The uploaded file is not a valid JPEG or PNG image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                String format = normalizeFormat(reader.getFormatName());
                if (!JPEG_FORMAT.equals(format) && !PNG_FORMAT.equals(format)) {
                    throw new InvalidImageException("Only JPEG and PNG images are supported");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new InvalidImageException("The uploaded image dimensions are invalid");
                }
                if (width > properties.getMaxWidth() || height > properties.getMaxHeight()
                        || (long) width * height > properties.getMaxPixels()) {
                    throw new InvalidImageException("The uploaded image dimensions are too large");
                }

                return new ImageUpload(content, format, width, height, content.length);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidImageException("The uploaded file is not a valid JPEG or PNG image");
        }
    }

    private String normalizeFormat(String formatName) {
        String normalized = formatName.toLowerCase(Locale.ROOT);

        return "jpeg".equals(normalized) ? JPEG_FORMAT : normalized;
    }
}
