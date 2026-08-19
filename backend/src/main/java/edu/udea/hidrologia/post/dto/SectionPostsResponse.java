package edu.udea.hidrologia.post.dto;

import java.util.List;

public record SectionPostsResponse(
        PostSectionResponse section,
        List<PostSummaryResponse> posts) {
}
