package edu.udea.hidrologia.link.service;

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

import edu.udea.hidrologia.link.dto.InterestingLinkResponse;
import edu.udea.hidrologia.link.entity.InterestingLink;
import edu.udea.hidrologia.link.repository.InterestingLinkRepository;

@ExtendWith(MockitoExtension.class)
class InterestingLinkServiceTest {

    @Mock
    private InterestingLinkRepository interestingLinkRepository;

    @InjectMocks
    private InterestingLinkService interestingLinkService;

    @Test
    void returnsEmptyListWhenThereAreNoActiveLinks() {
        when(interestingLinkRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of());

        List<InterestingLinkResponse> links = interestingLinkService.findActiveLinks();

        assertThat(links).isEmpty();
        verify(interestingLinkRepository).findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    @Test
    void returnsActiveLinksMappedToPublicDtos() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        InterestingLink link = new InterestingLink(
                1L,
                "Recurso aprobado",
                "Descripcion publica opcional",
                "https://example.edu/recurso",
                10,
                true,
                createdAt,
                createdAt);

        when(interestingLinkRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(link));

        List<InterestingLinkResponse> links = interestingLinkService.findActiveLinks();

        assertThat(links)
                .containsExactly(new InterestingLinkResponse(
                        1L,
                        "Recurso aprobado",
                        "Descripcion publica opcional",
                        "https://example.edu/recurso",
                        10));
        verify(interestingLinkRepository).findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }
}
