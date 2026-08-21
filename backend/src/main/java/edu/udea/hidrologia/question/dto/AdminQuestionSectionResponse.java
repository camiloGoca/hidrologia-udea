package edu.udea.hidrologia.question.dto;

import edu.udea.hidrologia.section.entity.SectionType;

public record AdminQuestionSectionResponse(
        Long id,
        SectionType type,
        String name,
        String slug,
        String description) {
}
