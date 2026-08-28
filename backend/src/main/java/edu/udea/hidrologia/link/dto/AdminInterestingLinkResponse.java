package edu.udea.hidrologia.link.dto;

public record AdminInterestingLinkResponse(
        Long id,
        String title,
        String description,
        String url,
        Integer displayOrder,
        boolean active) {
}
