package edu.udea.hidrologia.post.dto;

import java.util.List;

public record AdminPostsResponse(
        List<AdminPostSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
