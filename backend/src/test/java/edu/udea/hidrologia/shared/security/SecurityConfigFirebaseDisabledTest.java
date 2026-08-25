package edu.udea.hidrologia.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.udea.hidrologia.link.service.InterestingLinkService;
import edu.udea.hidrologia.post.service.AdminPostPublicationService;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.PostQueryService;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.question.service.AdminQuestionDraftService;
import edu.udea.hidrologia.question.service.AdminQuestionService;
import edu.udea.hidrologia.question.service.StudentQuestionService;
import edu.udea.hidrologia.section.repository.SectionRepository;

@SpringBootTest(properties = {
        "hidrologia.firebase.enabled=false",
        "hidrologia.firebase.admin-uid=admin-uid"
})
@AutoConfigureMockMvc
class SecurityConfigFirebaseDisabledTest {

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
    private StudentQuestionRepository studentQuestionRepository;

    @MockitoBean
    private QuestionAttachmentRepository questionAttachmentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doesNotBypassAdminAuthorizationWhenFirebaseIsDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                .header("Authorization", "Bearer any-token"))
                .andExpect(status().isUnauthorized());
    }
}
