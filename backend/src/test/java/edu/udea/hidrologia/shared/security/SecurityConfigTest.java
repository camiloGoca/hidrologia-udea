package edu.udea.hidrologia.shared.security;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.udea.hidrologia.link.service.InterestingLinkService;
import edu.udea.hidrologia.post.dto.PostDetailResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.SectionPostsResponse;
import edu.udea.hidrologia.post.dto.TagPostsResponse;
import edu.udea.hidrologia.post.dto.AdminPostImageResponse;
import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.CreatePostRequest;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.service.AdminPostPublicationService;
import edu.udea.hidrologia.post.service.AdminPostImageService;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.PostImageCleanupService;
import edu.udea.hidrologia.post.service.PostQueryService;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.question.service.AdminQuestionDraftService;
import edu.udea.hidrologia.question.service.AdminQuestionService;
import edu.udea.hidrologia.question.service.StudentQuestionService;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.firebase.FirebaseTokenVerificationException;
import edu.udea.hidrologia.shared.firebase.FirebaseTokenVerifier;
import edu.udea.hidrologia.shared.firebase.VerifiedFirebaseToken;
import edu.udea.hidrologia.tag.dto.AdminTagResponse;
import edu.udea.hidrologia.tag.dto.UpsertTagRequest;
import edu.udea.hidrologia.tag.service.AdminTagService;
import edu.udea.hidrologia.tag.dto.TagResponse;

@SpringBootTest(properties = "hidrologia.firebase.admin-uid=admin-uid")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private InterestingLinkService interestingLinkService;

    @MockitoBean
    private PostQueryService postQueryService;

    @MockitoBean
    private StudentQuestionService studentQuestionService;

    @MockitoBean
    private AdminQuestionService adminQuestionService;

    @MockitoBean
    private AdminQuestionDraftService adminQuestionDraftService;

    @MockitoBean
    private AdminPostService adminPostService;

    @MockitoBean
    private AdminPostPublicationService adminPostPublicationService;

    @MockitoBean
    private AdminPostImageService adminPostImageService;

    @MockitoBean
    private PostImageCleanupService postImageCleanupService;

    @MockitoBean
    private AdminTagService adminTagService;

    @MockitoBean
    private StudentQuestionRepository studentQuestionRepository;

    @MockitoBean
    private QuestionAttachmentRepository questionAttachmentRepository;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPublicLinksEndpoint() throws Exception {
        when(interestingLinkService.findActiveLinks()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicSectionPostsEndpoint() throws Exception {
        PostSectionResponse section = new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
        when(postQueryService.findPublishedPostsBySection("taller-1"))
                .thenReturn(new SectionPostsResponse(section, List.of()));

        mockMvc.perform(get("/api/v1/sections/taller-1/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicPostDetailEndpoint() throws Exception {
        PostSectionResponse section = new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
        when(postQueryService.findPublishedPostById(1L))
                .thenReturn(new PostDetailResponse(
                        1L,
                        "Titulo",
                        "Contenido",
                        contentDocument("Contenido"),
                        section,
                        List.of(),
                        List.of(),
                        null));

        mockMvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicTagPostsEndpoint() throws Exception {
        TagResponse tag = new TagResponse("Morfometria", "morfometria");
        when(postQueryService.findPublishedPostsByTag("morfometria"))
                .thenReturn(new TagPostsResponse(tag, List.of()));

        mockMvc.perform(get("/api/v1/tags/morfometria/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicQuestionSubmissionEndpoint() throws Exception {
        when(studentQuestionService.createQuestion(any(), any()))
                .thenReturn(new CreateStudentQuestionResponse(
                        1L,
                        StudentQuestionStatus.PENDING,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(multipart("/api/v1/questions")
                .file(new MockMultipartFile(
                        "data",
                        "data.json",
                        MediaType.APPLICATION_JSON_VALUE,
                        """
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "Pregunta de prueba"
                        }
                        """.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated());
    }

    @Test
    void deniesStandalonePublicImageUploadEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/images"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesUnapprovedApiRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/internal"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsUnauthorizedForAdminEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsUnauthorizedForMalformedBearerHeader() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                .header("Authorization", "Token invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsUnauthorizedForInvalidBearerToken() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(get("/api/v1/admin/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsUnauthorizedForRevokedBearerToken() throws Exception {
        when(firebaseTokenVerifier.verify(eq("revoked-token")))
                .thenThrow(new FirebaseTokenVerificationException("Revoked token", new RuntimeException()));

        mockMvc.perform(get("/api/v1/admin/me")
                .header("Authorization", "Bearer revoked-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsForbiddenForValidFirebaseTokenWithDifferentUid() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(get("/api/v1/admin/me")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void allowsAdminEndpointForConfiguredAdminUid() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));

        mockMvc.perform(get("/api/v1/admin/me")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void protectsPendingAdminQuestionsEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/questions/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsUnauthorizedForInvalidTokenOnAdminQuestionsEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(get("/api/v1/admin/questions/pending")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsForbiddenForNonAdminUidOnAdminQuestionsEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(get("/api/v1/admin/questions/pending")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void allowsAdminUidOnPendingAdminQuestionsEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));

        mockMvc.perform(get("/api/v1/admin/questions/pending")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsAdminUidOnQuestionDetailEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));

        mockMvc.perform(get("/api/v1/admin/questions/1")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void protectsAdminQuestionActionWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/questions/1/reject"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsUnauthorizedForInvalidTokenOnAdminQuestionAction() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(post("/api/v1/admin/questions/1/archive")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void returnsForbiddenForNonAdminUidOnAdminQuestionAction() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(post("/api/v1/admin/questions/1/reopen")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void allowsAdminUidOnQuestionStatusActions() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));

        mockMvc.perform(post("/api/v1/admin/questions/1/reject")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/questions/1/archive")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/questions/1/reopen")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void protectsQuestionDraftEndpointsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/questions/1/draft"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/questions/1/draft"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsAdminPostDetailWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posts/9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsUnauthorizedForInvalidTokenOnDraftEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(post("/api/v1/admin/questions/1/draft")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsForbiddenForNonAdminUidOnAdminPostEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(get("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminUidOnDraftAndAdminPostEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));
        when(adminQuestionDraftService.createDraft(1L))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "",
                        "",
                        contentDocument(""),
                        PostStatus.DRAFT,
                        1L,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        null));
        when(adminPostService.findAdminPostById(9L))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "",
                        "",
                        contentDocument(""),
                        PostStatus.DRAFT,
                        1L,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        null));

        mockMvc.perform(post("/api/v1/admin/questions/1/draft")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/admin/questions/1/draft")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void protectsAdminPostListUpdateAndActionsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/posts/9"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts/9/publish"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts/9/archive"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts/9/restore"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsUnauthorizedForInvalidTokenOnAdminPostListUpdateAndActions() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(get("/api/v1/admin/posts")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts/9/publish")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts/9/archive")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/posts/9/restore")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsForbiddenForNonAdminUidOnAdminPostListUpdateAndActions() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(get("/api/v1/admin/posts")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/posts")
                .header("Authorization", "Bearer other-user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer other-user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/posts/9/publish")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/posts/9/archive")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/posts/9/restore")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminUidOnAdminPostListUpdateAndActions() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));
        when(adminPostService.findPostsByStatus(PostStatus.DRAFT, 0, 20))
                .thenReturn(new AdminPostsResponse(List.of(), 0, 20, 0, 0));
        when(adminPostService.createManualDraft(any(CreatePostRequest.class)))
                .thenReturn(new AdminPostResponse(
                        10L,
                        "",
                        "",
                        contentDocument(""),
                        PostStatus.DRAFT,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        null));

        mockMvc.perform(get("/api/v1/admin/posts")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/posts")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Título",
                          "contentDocument": { "type": "doc", "content": [{ "type": "paragraph" }] },
                          "sectionSlug": "taller-1"
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/posts/9")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/posts/9/publish")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/posts/9/archive")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/posts/9/restore")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void protectsAdminPostImageEndpointsWithoutToken() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/posts/9/images")
                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3}))
                .param("altText", "Imagen"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/posts/9/images/15")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "altText": "Imagen"
                        }
                        """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsUnauthorizedForInvalidTokenOnAdminPostImageEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(multipart("/api/v1/admin/posts/9/images")
                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3}))
                .param("altText", "Imagen")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/posts/9/images/15")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "altText": "Imagen"
                        }
                        """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsForbiddenForNonAdminUidOnAdminPostImageEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(multipart("/api/v1/admin/posts/9/images")
                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3}))
                .param("altText", "Imagen")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/posts/9/images/15")
                .header("Authorization", "Bearer other-user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "altText": "Imagen"
                        }
                        """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminUidOnAdminPostImageEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));
        when(adminPostImageService.upload(eq(9L), any(), eq("Imagen")))
                .thenReturn(new AdminPostImageResponse(
                        15L,
                        "https://res.cloudinary.com/demo/image/upload/post.png",
                        "png",
                        800,
                        600,
                        1000L,
                        "Imagen",
                        Instant.parse("2026-01-01T00:00:00Z")));
        when(adminPostImageService.updateAltText(eq(9L), eq(15L), any()))
                .thenReturn(new AdminPostImageResponse(
                        15L,
                        "https://res.cloudinary.com/demo/image/upload/post.png",
                        "png",
                        800,
                        600,
                        1000L,
                        "Imagen actualizada",
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(multipart("/api/v1/admin/posts/9/images")
                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3}))
                .param("altText", "Imagen")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/admin/posts/9/images/15")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "altText": "Imagen actualizada"
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void protectsAdminTagEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tags"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/tags/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/tags/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsUnauthorizedForInvalidTokenOnAdminTagEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("invalid-token")))
                .thenThrow(new FirebaseTokenVerificationException("Invalid token", new RuntimeException()));

        mockMvc.perform(get("/api/v1/admin/tags")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/tags")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/tags/1")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/tags/1")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsForbiddenForNonAdminUidOnAdminTagEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("other-user-token")))
                .thenReturn(new VerifiedFirebaseToken("other-uid"));

        mockMvc.perform(get("/api/v1/admin/tags")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/tags")
                .header("Authorization", "Bearer other-user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/tags/1")
                .header("Authorization", "Bearer other-user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/tags/1")
                .header("Authorization", "Bearer other-user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminUidOnAdminTagEndpoints() throws Exception {
        when(firebaseTokenVerifier.verify(eq("admin-token")))
                .thenReturn(new VerifiedFirebaseToken("admin-uid"));
        when(adminTagService.create(any(UpsertTagRequest.class)))
                .thenReturn(new AdminTagResponse(1L, "Morfometría", "morfometria", 0));
        when(adminTagService.rename(eq(1L), any(UpsertTagRequest.class)))
                .thenReturn(new AdminTagResponse(1L, "Morfometría de cuencas", "morfometria", 0));

        mockMvc.perform(get("/api/v1/admin/tags")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/tags")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Morfometría"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/admin/tags/1")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Morfometría de cuencas"
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/tags/1")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());
    }

    private Map<String, Object> contentDocument(String content) {
        return Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", content)))));
    }
}
