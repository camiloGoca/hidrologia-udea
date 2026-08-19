package edu.udea.hidrologia.post.dto;

import java.time.Instant;
import java.util.List;

import edu.udea.hidrologia.tag.dto.TagResponse;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        PostSectionResponse section,
        List<TagResponse> tags,
        Instant publishedAt) {
}
