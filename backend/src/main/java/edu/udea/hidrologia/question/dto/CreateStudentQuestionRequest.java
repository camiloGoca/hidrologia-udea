package edu.udea.hidrologia.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentQuestionRequest(
        @NotBlank(message = "Section is required")
        @Size(max = 140, message = "Section is too long")
        String sectionSlug,

        @Size(max = 80, message = "Nickname must be 80 characters or less")
        String nickname,

        @NotBlank(message = "Question is required")
        @Size(max = 2000, message = "Question must be 2000 characters or less")
        String question,

        String turnstileToken) {
}
