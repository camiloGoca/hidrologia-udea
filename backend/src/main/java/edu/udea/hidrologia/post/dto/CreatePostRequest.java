package edu.udea.hidrologia.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank
        @Size(min = 1, max = 120)
        String sectionSlug) {
}
