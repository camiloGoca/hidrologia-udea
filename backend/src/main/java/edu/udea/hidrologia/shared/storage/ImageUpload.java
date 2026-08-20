package edu.udea.hidrologia.shared.storage;

public record ImageUpload(
        byte[] content,
        String format,
        int width,
        int height,
        long bytes) {
}
