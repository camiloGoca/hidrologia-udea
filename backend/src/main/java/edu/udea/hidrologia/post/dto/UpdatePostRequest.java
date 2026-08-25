package edu.udea.hidrologia.post.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotNull
        @Size(max = 180)
        String title,

        @NotNull
        String content,

        @NotBlank
        @Size(min = 1, max = 120)
        String sectionSlug,

        @Valid
        List<@Positive Long> tagIds) {
}
