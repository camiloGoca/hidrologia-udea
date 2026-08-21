package edu.udea.hidrologia.shared.security;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

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
import edu.udea.hidrologia.post.service.PostQueryService;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.question.service.AdminQuestionService;
import edu.udea.hidrologia.question.service.StudentQuestionService;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.firebase.FirebaseTokenVerificationException;
import edu.udea.hidrologia.shared.firebase.FirebaseTokenVerifier;
import edu.udea.hidrologia.shared.firebase.VerifiedFirebaseToken;
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
                .thenReturn(new PostDetailResponse(1L, "Titulo", "Contenido", section, List.of(), null));

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
}
