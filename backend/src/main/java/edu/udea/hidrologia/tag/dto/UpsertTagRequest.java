package edu.udea.hidrologia.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertTagRequest(
        @NotBlank
        @Size(max = 80)
        String name) {
}
