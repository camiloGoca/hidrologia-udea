package edu.udea.hidrologia.shared.storage;

public record StoredImage(
        String publicId,
        String secureUrl,
        String format,
        int width,
        int height,
        long bytes) {
}
