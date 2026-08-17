package edu.udea.hidrologia.section.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.udea.hidrologia.section.dto.SectionResponse;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private SectionService sectionService;

    @Test
    void returnsActiveSectionsOrderedAsRepositoryProvidesThem() {
        Section taller = new Section(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometría de cuencas",
                1,
                true,
                Instant.parse("2026-01-01T00:00:00Z"));
        Section parcial = new Section(
                4L,
                SectionType.PARCIAL,
                "Parcial 1",
                "parcial-1",
                null,
                4,
                true,
                Instant.parse("2026-01-01T00:00:00Z"));

        when(sectionRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(taller, parcial));

        List<SectionResponse> sections = sectionService.findActiveSections();

        assertThat(sections)
                .containsExactly(
                        new SectionResponse(
                                1L,
                                SectionType.TALLER,
                                "Taller 1",
                                "taller-1",
                                "Morfometría de cuencas",
                                1),
                        new SectionResponse(
                                4L,
                                SectionType.PARCIAL,
                                "Parcial 1",
                                "parcial-1",
                                null,
                                4));
        verify(sectionRepository).findByActiveTrueOrderByDisplayOrderAsc();
    }
}
