package edu.udea.hidrologia.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import edu.udea.hidrologia.tag.dto.TagResponse;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        Map<String, Object> contentDocument,
        PostSectionResponse section,
        List<TagResponse> tags,
        List<PostImageResponse> images,
        Instant publishedAt) {
}
