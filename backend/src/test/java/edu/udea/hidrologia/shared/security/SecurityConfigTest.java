package edu.udea.hidrologia.shared.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.udea.hidrologia.link.service.InterestingLinkService;
import edu.udea.hidrologia.section.repository.SectionRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private InterestingLinkService interestingLinkService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPublicLinksEndpoint() throws Exception {
        when(interestingLinkService.findActiveLinks()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk());
    }

    @Test
    void deniesUnapprovedApiRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/links"))
                .andExpect(status().isForbidden());
    }
}
