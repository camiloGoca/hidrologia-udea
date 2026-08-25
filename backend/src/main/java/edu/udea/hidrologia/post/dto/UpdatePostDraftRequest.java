package edu.udea.hidrologia.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostDraftRequest(
        @NotNull
        @Size(max = 180)
        String title,

        @NotNull
        String content,

        @NotBlank
        @Size(min = 1, max = 120)
        String sectionSlug) {
}
