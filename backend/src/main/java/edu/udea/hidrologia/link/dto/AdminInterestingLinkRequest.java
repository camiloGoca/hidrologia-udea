package edu.udea.hidrologia.link.dto;

public record AdminInterestingLinkRequest(
        String title,
        String description,
        String url,
        Integer displayOrder,
        Boolean active) {
}
