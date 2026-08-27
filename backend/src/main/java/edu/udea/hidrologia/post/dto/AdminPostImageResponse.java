package edu.udea.hidrologia.post.dto;

import java.time.Instant;

public record AdminPostImageResponse(
        Long id,
        String secureUrl,
        String format,
        Integer width,
        Integer height,
        Long bytes,
        String altText,
        Instant createdAt) {
}
