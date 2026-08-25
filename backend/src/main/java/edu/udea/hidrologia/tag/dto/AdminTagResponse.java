package edu.udea.hidrologia.tag.dto;

public record AdminTagResponse(
        Long id,
        String name,
        String slug,
        long usageCount) {
}
