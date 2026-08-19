package edu.udea.hidrologia.link.dto;

public record InterestingLinkResponse(
        Long id,
        String title,
        String description,
        String url,
        Integer displayOrder) {
}
