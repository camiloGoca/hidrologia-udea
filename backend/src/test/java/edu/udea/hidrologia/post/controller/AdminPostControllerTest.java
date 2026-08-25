package edu.udea.hidrologia.post.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.UpdatePostDraftRequest;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.service.AdminPostPublicationService;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.InvalidPostPublicationException;
import edu.udea.hidrologia.post.service.PostStateConflictException;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AdminPostControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AdminPostService adminPostService;
    private AdminPostPublicationService adminPostPublicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminPostService = Mockito.mock(AdminPostService.class);
        adminPostPublicationService = Mockito.mock(AdminPostPublicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPostController(
                        adminPostService,
                        adminPostPublicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAdminPostDetail() throws Exception {
        when(adminPostService.findAdminPostById(9L))
                .thenReturn(draftResponse());

        mockMvc.perform(get("/api/v1/admin/posts/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(9)))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.sourceQuestionId", is(1)))
                .andExpect(jsonPath("$.sourceQuestion.question", is("Pregunta original")))
                .andExpect(jsonPath("$.sourceQuestion.hasAttachment", is(true)))
                .andExpect(jsonPath("$.sourceQuestion.publicId").doesNotExist())
                .andExpect(jsonPath("$.publishedAt").doesNotExist());
    }

    @Test
    void updatesDraftPost() throws Exception {
        when(adminPostService.updateDraft(Mockito.eq(9L), Mockito.any(UpdatePostDraftRequest.class)))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "Título guardado",
                        "Línea 1\nLínea 2",
                        PostStatus.DRAFT,
                        1L,
                        section(),
                        sourceQuestion(),
                        NOW,
                        NOW,
                        null));

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "  Título guardado  ",
                          "content": "  Línea 1\\nLínea 2  ",
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Título guardado")))
                .andExpect(jsonPath("$.content", is("Línea 1\nLínea 2")))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void returnsBadRequestForInvalidDraftUpdateRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": null,
                          "content": "Contenido",
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenDraftUpdateIncludesStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "content": "Contenido",
                          "sectionSlug": "taller-1",
                          "status": "PUBLISHED"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsConflictWhenDraftUpdateStateIsInvalid() throws Exception {
        when(adminPostService.updateDraft(Mockito.eq(9L), Mockito.any(UpdatePostDraftRequest.class)))
                .thenThrow(new PostStateConflictException("Only draft posts can be edited"));

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "content": "Contenido",
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Only draft posts can be edited")));
    }

    @Test
    void publishesDraftPost() throws Exception {
        when(adminPostPublicationService.publishDraft(9L))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "Título publicado",
                        "Contenido",
                        PostStatus.PUBLISHED,
                        1L,
                        section(),
                        sourceQuestion(),
                        NOW,
                        NOW,
                        NOW));

        mockMvc.perform(post("/api/v1/admin/posts/9/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", is("2026-01-01T00:00:00Z")));
    }

    @Test
    void returnsBadRequestWhenDraftIsNotPublishable() throws Exception {
        when(adminPostPublicationService.publishDraft(9L))
                .thenThrow(new InvalidPostPublicationException(
                        "El borrador necesita título y contenido antes de publicarse."));

        mockMvc.perform(post("/api/v1/admin/posts/9/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El borrador necesita título y contenido antes de publicarse.")));
    }

    @Test
    void returnsNotFoundForMissingAdminPost() throws Exception {
        when(adminPostService.findAdminPostById(404L)).thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/v1/admin/posts/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Post not found")));
    }

    private AdminPostResponse draftResponse() {
        return new AdminPostResponse(
                9L,
                "",
                "",
                PostStatus.DRAFT,
                1L,
                section(),
                sourceQuestion(),
                NOW,
                NOW,
                null);
    }

    private PostSectionResponse section() {
        return new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
    }

    private AdminPostSourceQuestionResponse sourceQuestion() {
        return new AdminPostSourceQuestionResponse(
                1L,
                null,
                "Pregunta original",
                StudentQuestionStatus.PENDING,
                NOW,
                true);
    }
}
