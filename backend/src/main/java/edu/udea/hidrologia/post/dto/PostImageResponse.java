package edu.udea.hidrologia.post.dto;

public record PostImageResponse(
        Long id,
        String secureUrl,
        Integer width,
        Integer height,
        String altText) {
}
