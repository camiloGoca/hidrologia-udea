package edu.udea.hidrologia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import edu.udea.hidrologia.analytics.repository.AnalyticsRepository;
import edu.udea.hidrologia.link.repository.InterestingLinkRepository;
import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.controller.AdminPostController;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.tag.repository.TagRepository;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class HidrologiaApplicationTests {

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private InterestingLinkRepository interestingLinkRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private PostImageRepository postImageRepository;

    @MockitoBean
    private TagRepository tagRepository;

    @MockitoBean
    private StudentQuestionRepository studentQuestionRepository;

    @MockitoBean
    private QuestionAttachmentRepository questionAttachmentRepository;

    @MockitoBean
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext.getBean(JsonMapper.class)).isNotNull();
        assertThat(applicationContext.getBean(PostContentDocumentService.class)).isNotNull();
        assertThat(applicationContext.getBean(AdminPostController.class)).isNotNull();
    }
}
