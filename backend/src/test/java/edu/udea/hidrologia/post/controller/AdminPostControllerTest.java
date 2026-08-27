package edu.udea.hidrologia.post.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.json.JsonMapper;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.AdminPostSummaryResponse;
import edu.udea.hidrologia.post.dto.AdminPostTagResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.CreatePostRequest;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.UpdatePostRequest;
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
    private static final PostContentDocumentService POST_CONTENT_DOCUMENT_SERVICE =
            new PostContentDocumentService(JsonMapper.builder().build());

    private AdminPostService adminPostService;
    private AdminPostPublicationService adminPostPublicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminPostService = Mockito.mock(AdminPostService.class);
        adminPostPublicationService = Mockito.mock(AdminPostPublicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPostController(
                        adminPostService,
                        adminPostPublicationService,
                        POST_CONTENT_DOCUMENT_SERVICE))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAdminPostsByStatus() throws Exception {
        when(adminPostService.findPostsByStatus(PostStatus.PUBLISHED, 2, 10))
                .thenReturn(new AdminPostsResponse(
                        List.of(new AdminPostSummaryResponse(
                                9L,
                                "Título publicado",
                                PostStatus.PUBLISHED,
                                section(),
                                true,
                                1L,
                                NOW,
                                NOW,
                                NOW)),
                        2,
                        10,
                        21,
                        3));

        mockMvc.perform(get("/api/v1/admin/posts?status=PUBLISHED&page=2&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(9)))
                .andExpect(jsonPath("$.items[0].title", is("Título publicado")))
                .andExpect(jsonPath("$.items[0].status", is("PUBLISHED")))
                .andExpect(jsonPath("$.items[0].section.slug", is("taller-1")))
                .andExpect(jsonPath("$.items[0].hasSourceQuestion", is(true)))
                .andExpect(jsonPath("$.items[0].sourceQuestionId", is(1)))
                .andExpect(jsonPath("$.items[0].createdAt", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.items[0].updatedAt", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.items[0].publishedAt", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.page", is(2)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", is(21)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void defaultsAdminPostListToDrafts() throws Exception {
        when(adminPostService.findPostsByStatus(PostStatus.DRAFT, 0, 20))
                .thenReturn(new AdminPostsResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void returnsBadRequestForInvalidAdminPostStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posts?status=UNKNOWN"))
                .andExpect(status().isBadRequest());
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
    void createsManualDraftPost() throws Exception {
        when(adminPostService.createManualDraft(Mockito.any(CreatePostRequest.class)))
                .thenReturn(manualDraftResponse());

        mockMvc.perform(post("/api/v1/admin/posts")
                .contentType("application/json")
                .content("""
                        {
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.title", is("")))
                .andExpect(jsonPath("$.content", is("")))
                .andExpect(jsonPath("$.sourceQuestionId").doesNotExist())
                .andExpect(jsonPath("$.sourceQuestion").doesNotExist())
                .andExpect(jsonPath("$.publishedAt").doesNotExist())
                .andExpect(jsonPath("$.tags", hasSize(0)));
    }

    @Test
    void rejectsManualDraftCreateWithUnsupportedFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/posts")
                .contentType("application/json")
                .content("""
                        {
                          "sectionSlug": "taller-1",
                          "status": "PUBLISHED",
                          "sourceQuestionId": 1,
                          "publishedAt": "2026-01-01T00:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsManualDraftCreateWithoutValidSection() throws Exception {
        mockMvc.perform(post("/api/v1/admin/posts")
                .contentType("application/json")
                .content("""
                        {
                          "sectionSlug": ""
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void discardsManualDraftPost() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/posts/10"))
                .andExpect(status().isNoContent());

        Mockito.verify(adminPostService).discardManualDraft(10L);
    }

    @Test
    void updatesPost() throws Exception {
        when(adminPostService.updatePost(Mockito.eq(9L), Mockito.any(UpdatePostRequest.class)))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "Título guardado",
                        "Línea 1\nLínea 2",
                        contentDocument("Línea 1\nLínea 2"),
                        PostStatus.PUBLISHED,
                        1L,
                        section(),
                        sourceQuestion(),
                        List.of(new AdminPostTagResponse(1L, "Morfometría", "morfometria")),
                        List.of(),
                        NOW,
                        NOW,
                        NOW));

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "  Título guardado  ",
                          "contentDocument": {
                            "type": "doc",
                            "content": [
                              {
                                "type": "paragraph",
                                "content": [{ "type": "text", "text": "Línea 1" }]
                              }
                            ]
                          },
                          "sectionSlug": "taller-1",
                          "tagIds": [1, 1]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Título guardado")))
                .andExpect(jsonPath("$.content", is("Línea 1\nLínea 2")))
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.tags[0].id", is(1)))
                .andExpect(jsonPath("$.tags[0].slug", is("morfometria")));
    }

    @Test
    void returnsBadRequestForInvalidUpdateRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": null,
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenUpdateIncludesStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1",
                          "status": "PUBLISHED"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsTagIdsToBeOmittedForCompatibility() throws Exception {
        when(adminPostService.updatePost(Mockito.eq(9L), Mockito.any(UpdatePostRequest.class)))
                .thenReturn(draftResponse());

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void acceptsEmptyTagIdsToRemoveAllTags() throws Exception {
        when(adminPostService.updatePost(Mockito.eq(9L), Mockito.any(UpdatePostRequest.class)))
                .thenReturn(draftResponse());

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1",
                          "tagIds": []
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidTagIds() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1",
                          "tagIds": [0]
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForUnsafeLinkDocument() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": {
                            "type": "doc",
                            "content": [
                              {
                                "type": "paragraph",
                                "content": [
                                  {
                                    "type": "text",
                                    "text": "Link",
                                    "marks": [
                                      {
                                        "type": "link",
                                        "attrs": { "href": "javascript:alert(1)" }
                                      }
                                    ]
                                  }
                                ]
                              }
                            ]
                          },
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(adminPostService);
    }

    @Test
    void returnsBadRequestWhenUpdateIncludesUnsupportedFields() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "content": "Contenido",
                          "sectionSlug": "taller-1",
                          "publishedAt": "2026-01-01T00:00:00Z",
                          "sourceQuestionId": 1,
                          "tags": [],
                          "attachments": [],
                          "public_id": "private-id"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsConflictWhenUpdateStateIsInvalid() throws Exception {
        when(adminPostService.updatePost(Mockito.eq(9L), Mockito.any(UpdatePostRequest.class)))
                .thenThrow(new PostStateConflictException("Only draft posts can be edited"));

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType("application/json")
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Only draft posts can be edited")));
    }

    @Test
    void publishesDraftPost() throws Exception {
        when(adminPostPublicationService.publishDraft(9L))
                .thenReturn(publishedResponse());

        mockMvc.perform(post("/api/v1/admin/posts/9/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", is("2026-01-01T00:00:00Z")));
    }

    @Test
    void archivesPublishedPost() throws Exception {
        when(adminPostPublicationService.archivePost(9L))
                .thenReturn(archivedResponse());

        mockMvc.perform(post("/api/v1/admin/posts/9/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")))
                .andExpect(jsonPath("$.publishedAt", is("2026-01-01T00:00:00Z")));
    }

    @Test
    void restoresArchivedPost() throws Exception {
        when(adminPostPublicationService.restorePost(9L))
                .thenReturn(publishedResponse());

        mockMvc.perform(post("/api/v1/admin/posts/9/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", is("2026-01-01T00:00:00Z")));
    }

    @Test
    void returnsBadRequestWhenPostIsNotPublishable() throws Exception {
        when(adminPostPublicationService.publishDraft(9L))
                .thenThrow(new InvalidPostPublicationException(
                        "La publicación necesita título y contenido antes de publicarse."));

        mockMvc.perform(post("/api/v1/admin/posts/9/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("La publicación necesita título y contenido antes de publicarse.")));
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
                contentDocument(""),
                PostStatus.DRAFT,
                1L,
                section(),
                sourceQuestion(),
                List.of(),
                List.of(),
                NOW,
                NOW,
                null);
    }

    private AdminPostResponse manualDraftResponse() {
        return new AdminPostResponse(
                10L,
                "",
                "",
                contentDocument(""),
                PostStatus.DRAFT,
                null,
                section(),
                null,
                List.of(),
                List.of(),
                NOW,
                NOW,
                null);
    }

    private AdminPostResponse publishedResponse() {
        return new AdminPostResponse(
                9L,
                "Título publicado",
                "Contenido",
                contentDocument("Contenido"),
                PostStatus.PUBLISHED,
                1L,
                section(),
                sourceQuestion(),
                List.of(),
                List.of(),
                NOW,
                NOW,
                NOW);
    }

    private AdminPostResponse archivedResponse() {
        return new AdminPostResponse(
                9L,
                "Título publicado",
                "Contenido",
                contentDocument("Contenido"),
                PostStatus.ARCHIVED,
                1L,
                section(),
                sourceQuestion(),
                List.of(),
                List.of(),
                NOW,
                NOW,
                NOW);
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

    private Map<String, Object> contentDocument(String content) {
        return Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", content)))));
    }
}
