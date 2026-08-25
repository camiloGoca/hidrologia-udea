package edu.udea.hidrologia.question.dto;

import edu.udea.hidrologia.post.entity.PostStatus;

public record AdminLinkedPostResponse(
        Long id,
        PostStatus status,
        String title) {
}
