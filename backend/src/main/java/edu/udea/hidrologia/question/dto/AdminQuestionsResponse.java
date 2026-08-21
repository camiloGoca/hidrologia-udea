package edu.udea.hidrologia.question.dto;

import java.util.List;

public record AdminQuestionsResponse(
        List<AdminQuestionSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
