package edu.udea.hidrologia.shared.health.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import edu.udea.hidrologia.link.repository.InterestingLinkRepository;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.tag.repository.TagRepository;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private InterestingLinkRepository interestingLinkRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private TagRepository tagRepository;

    @MockitoBean
    private StudentQuestionRepository studentQuestionRepository;

    @MockitoBean
    private QuestionAttachmentRepository questionAttachmentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsApiHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.message", is("Hidrologia UdeA API is running")));
    }
}
