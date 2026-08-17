package edu.udea.hidrologia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import edu.udea.hidrologia.section.repository.SectionRepository;

@SpringBootTest
class HidrologiaApplicationTests {

    @MockitoBean
    private SectionRepository sectionRepository;

    @Test
    void contextLoads() {
    }
}
