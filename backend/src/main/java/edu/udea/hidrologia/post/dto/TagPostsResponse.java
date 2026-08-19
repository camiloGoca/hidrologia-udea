package edu.udea.hidrologia.post.dto;

import java.util.List;

import edu.udea.hidrologia.tag.dto.TagResponse;

public record TagPostsResponse(
        TagResponse tag,
        List<PostSummaryResponse> posts) {
}
