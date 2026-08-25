package edu.udea.hidrologia.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminPostServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private AdminPostService adminPostService;

    @Test
    void returnsAdminPostWithSourceQuestionReference() {
        StudentQuestion question = withAttachment(question());
        Post draft = new Post(
                9L,
                question.getSection(),
                "",
                "",
                PostStatus.DRAFT,
                NOW,
                NOW,
                null,
                Set.of(),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));

        AdminPostResponse response = adminPostService.findAdminPostById(9L);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.status()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.title()).isEmpty();
        assertThat(response.content()).isEmpty();
        assertThat(response.sourceQuestionId()).isEqualTo(1L);
        assertThat(response.sourceQuestion().question()).isEqualTo("Pregunta original");
        assertThat(response.sourceQuestion().hasAttachment()).isTrue();
        assertThat(response.section().slug()).isEqualTo("taller-1");
    }

    @Test
    void throwsNotFoundWhenAdminPostDoesNotExist() {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPostService.findAdminPostById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
    }

    @Test
    void allowsDraftToExistWithBlankTitleAndContentButRejectsPublishedBlankContent() {
        StudentQuestion question = question();

        Post draft = Post.createQuestionDraft(question, NOW);

        assertThat(draft.getTitle()).isEmpty();
        assertThat(draft.getContent()).isEmpty();
        assertThatThrownBy(() -> new Post(
                9L,
                question.getSection(),
                "",
                "",
                PostStatus.PUBLISHED,
                NOW,
                NOW,
                NOW,
                Set.of(),
                question))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private StudentQuestion withAttachment(StudentQuestion question) {
        QuestionAttachment attachment = new QuestionAttachment(
                1L,
                question,
                "hidrologia-udea/questions/private-id",
                "https://res.cloudinary.com/demo/image/upload/question.png",
                "png",
                640,
                480,
                1000L,
                NOW);
        ReflectionTestUtils.setField(question, "attachment", attachment);

        return question;
    }

    private StudentQuestion question() {
        return new StudentQuestion(
                1L,
                section(),
                null,
                "Pregunta original",
                StudentQuestionStatus.PENDING,
                NOW,
                NOW);
    }

    private Section section() {
        return new Section(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas",
                1,
                true,
                NOW);
    }
}
