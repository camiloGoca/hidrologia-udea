package edu.udea.hidrologia.section.dto;

import edu.udea.hidrologia.section.entity.SectionType;

public record SectionResponse(
        Long id,
        SectionType type,
        String name,
        String slug,
        String description,
        Integer displayOrder) {
}
