package edu.udea.hidrologia.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class AdminPostService {

    private final PostRepository postRepository;

    public AdminPostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public AdminPostResponse findAdminPostById(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        return toResponse(post);
    }

    public AdminPostResponse toResponse(Post post) {
        StudentQuestion sourceQuestion = post.getSourceQuestion();

        return new AdminPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                sourceQuestion == null ? null : sourceQuestion.getId(),
                toSectionResponse(post.getSection()),
                sourceQuestion == null ? null : toSourceQuestionResponse(sourceQuestion),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private AdminPostSourceQuestionResponse toSourceQuestionResponse(StudentQuestion question) {
        return new AdminPostSourceQuestionResponse(
                question.getId(),
                question.getNickname(),
                question.getQuestion(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getAttachment() != null);
    }

    private PostSectionResponse toSectionResponse(Section section) {
        return new PostSectionResponse(
                section.getId(),
                section.getType(),
                section.getName(),
                section.getSlug(),
                section.getDescription());
    }
}
