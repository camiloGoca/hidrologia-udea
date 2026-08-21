package edu.udea.hidrologia.question.dto;

public record AdminQuestionAttachmentResponse(
        String secureUrl,
        String format,
        Integer width,
        Integer height,
        Long bytes) {
}
