package edu.udea.hidrologia.question.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.question.dto.AdminQuestionAttachmentResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSectionResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionStatusUpdateResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSummaryResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionsResponse;
import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.AdminQuestionDraftService;
import edu.udea.hidrologia.question.service.AdminQuestionService;
import edu.udea.hidrologia.question.service.InvalidQuestionStatusTransitionException;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AdminQuestionControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AdminQuestionService adminQuestionService;
    private AdminQuestionDraftService adminQuestionDraftService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminQuestionService = Mockito.mock(AdminQuestionService.class);
        adminQuestionDraftService = Mockito.mock(AdminQuestionDraftService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminQuestionController(
                        adminQuestionService,
                        adminQuestionDraftService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsQuestionsByStatus() throws Exception {
        when(adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.ARCHIVED, 2, 10))
                .thenReturn(new AdminQuestionsResponse(
                        List.of(new AdminQuestionSummaryResponse(
                                1L,
                                null,
                                section(),
                                StudentQuestionStatus.ARCHIVED,
                                "Pregunta",
                                true,
                                false,
                                NOW)),
                        2,
                        10,
                        21,
                        3));

        mockMvc.perform(get("/api/v1/admin/questions?status=ARCHIVED&page=2&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(1)))
                .andExpect(jsonPath("$.items[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.items[0].status", is("ARCHIVED")))
                .andExpect(jsonPath("$.items[0].questionPreview", is("Pregunta")))
                .andExpect(jsonPath("$.items[0].hasAttachment", is(true)))
                .andExpect(jsonPath("$.items[0].section.slug", is("taller-1")))
                .andExpect(jsonPath("$.page", is(2)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", is(21)))
                .andExpect(jsonPath("$.totalPages", is(3)));

        verify(adminQuestionService).findQuestionsByStatus(StudentQuestionStatus.ARCHIVED, 2, 10);
    }

    @Test
    void keepsPendingEndpointForCompatibility() throws Exception {
        when(adminQuestionService.findPendingQuestions(0, 20))
                .thenReturn(new AdminQuestionsResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/admin/questions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(adminQuestionService).findPendingQuestions(0, 20);
    }

    @Test
    void returnsPublishedQuestionsByStatus() throws Exception {
        when(adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.PUBLISHED, 0, 20))
                .thenReturn(new AdminQuestionsResponse(
                        List.of(new AdminQuestionSummaryResponse(
                                1L,
                                null,
                                section(),
                                StudentQuestionStatus.PUBLISHED,
                                "Pregunta",
                                false,
                                true,
                                NOW)),
                        0,
                        20,
                        1,
                        1));

        mockMvc.perform(get("/api/v1/admin/questions?status=PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status", is("PUBLISHED")))
                .andExpect(jsonPath("$.items[0].hasLinkedPost", is(true)));
    }

    @Test
    void returnsQuestionDetailWithAttachment() throws Exception {
        when(adminQuestionService.findQuestionById(1L))
                .thenReturn(new AdminQuestionDetailResponse(
                        1L,
                        "Goca",
                        "Pregunta completa",
                        StudentQuestionStatus.PENDING,
                        NOW,
                        NOW.plusSeconds(30),
                        section(),
                        new AdminQuestionAttachmentResponse(
                                "https://res.cloudinary.com/demo/image/upload/question.png",
                                "png",
                                640,
                                480,
                                1000L),
                        null));

        mockMvc.perform(get("/api/v1/admin/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nickname", is("Goca")))
                .andExpect(jsonPath("$.question", is("Pregunta completa")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.attachment.secureUrl", is("https://res.cloudinary.com/demo/image/upload/question.png")))
                .andExpect(jsonPath("$.attachment.publicId").doesNotExist());
    }

    @Test
    void returnsNotFoundForMissingQuestion() throws Exception {
        when(adminQuestionService.findQuestionById(404L)).thenThrow(new ResourceNotFoundException("Question not found"));

        mockMvc.perform(get("/api/v1/admin/questions/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Question not found")));
    }

    @Test
    void rejectsQuestion() throws Exception {
        when(adminQuestionService.rejectQuestion(1L))
                .thenReturn(new AdminQuestionStatusUpdateResponse(1L, StudentQuestionStatus.REJECTED, NOW));

        mockMvc.perform(post("/api/v1/admin/questions/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    void archivesQuestion() throws Exception {
        when(adminQuestionService.archiveQuestion(1L))
                .thenReturn(new AdminQuestionStatusUpdateResponse(1L, StudentQuestionStatus.ARCHIVED, NOW));

        mockMvc.perform(post("/api/v1/admin/questions/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")));
    }

    @Test
    void reopensQuestion() throws Exception {
        when(adminQuestionService.reopenQuestion(1L))
                .thenReturn(new AdminQuestionStatusUpdateResponse(1L, StudentQuestionStatus.PENDING, NOW));

        mockMvc.perform(post("/api/v1/admin/questions/1/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void createsQuestionDraftWithLocationHeader() throws Exception {
        when(adminQuestionDraftService.createDraft(1L))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "",
                        "",
                        PostStatus.DRAFT,
                        1L,
                        null,
                        null,
                        java.util.List.of(),
                        NOW,
                        NOW,
                        null));

        mockMvc.perform(post("/api/v1/admin/questions/1/draft"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(9)))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.sourceQuestionId", is(1)))
                .andExpect(jsonPath("$.title", is("")))
                .andExpect(jsonPath("$.content", is("")))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getHeader("Location"))
                        .isEqualTo("/api/v1/admin/posts/9"));

        verify(adminQuestionDraftService).createDraft(1L);
    }

    @Test
    void discardsQuestionDraft() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/questions/1/draft"))
                .andExpect(status().isNoContent());

        verify(adminQuestionDraftService).discardDraft(1L);
    }

    @Test
    void returnsConflictForInvalidTransition() throws Exception {
        when(adminQuestionService.archiveQuestion(1L)).thenThrow(new InvalidQuestionStatusTransitionException());

        mockMvc.perform(post("/api/v1/admin/questions/1/archive"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    private AdminQuestionSectionResponse section() {
        return new AdminQuestionSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
    }
}
