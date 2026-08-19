package edu.udea.hidrologia.post.dto;

import edu.udea.hidrologia.section.entity.SectionType;

public record PostSectionResponse(
        Long id,
        SectionType type,
        String name,
        String slug,
        String description) {
}
