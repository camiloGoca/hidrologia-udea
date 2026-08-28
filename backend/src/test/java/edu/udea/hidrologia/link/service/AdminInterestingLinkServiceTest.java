package edu.udea.hidrologia.link.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.udea.hidrologia.link.dto.AdminInterestingLinkRequest;
import edu.udea.hidrologia.link.dto.AdminInterestingLinkResponse;
import edu.udea.hidrologia.link.entity.InterestingLink;
import edu.udea.hidrologia.link.repository.InterestingLinkRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminInterestingLinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private InterestingLinkRepository interestingLinkRepository;

    private AdminInterestingLinkService service;

    @BeforeEach
    void setUp() {
        service = new AdminInterestingLinkService(
                interestingLinkRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listsAllLinksOrderedByDisplayOrderAndId() {
        InterestingLink link = link(1L, "Recurso", "https://example.edu", 2, false);
        when(interestingLinkRepository.findAllByOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(link));

        List<AdminInterestingLinkResponse> response = service.findAll();

        assertThat(response).containsExactly(new AdminInterestingLinkResponse(
                1L,
                "Recurso",
                "Descripcion",
                "https://example.edu",
                2,
                false));
        verify(interestingLinkRepository).findAllByOrderByDisplayOrderAscIdAsc();
    }

    @Test
    void createsLinkWithTrimmedValuesAndDefaults() {
        when(interestingLinkRepository.saveAndFlush(any(InterestingLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminInterestingLinkResponse response = service.create(new AdminInterestingLinkRequest(
                "  Recurso externo  ",
                "  Guia opcional  ",
                "  https://example.edu/recurso  ",
                null,
                null));

        assertThat(response.title()).isEqualTo("Recurso externo");
        assertThat(response.description()).isEqualTo("Guia opcional");
        assertThat(response.url()).isEqualTo("https://example.edu/recurso");
        assertThat(response.displayOrder()).isZero();
        assertThat(response.active()).isTrue();

        ArgumentCaptor<InterestingLink> captor = ArgumentCaptor.forClass(InterestingLink.class);
        verify(interestingLinkRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void updatesLinkAndPreservesDefaultsWhenOptionalFieldsAreOmitted() {
        InterestingLink link = link(7L, "Anterior", "https://example.edu/old", 5, false);
        when(interestingLinkRepository.findById(7L)).thenReturn(Optional.of(link));

        AdminInterestingLinkResponse response = service.update(7L, new AdminInterestingLinkRequest(
                " Nuevo ",
                "   ",
                "https://example.edu/nuevo",
                null,
                null));

        assertThat(response.title()).isEqualTo("Nuevo");
        assertThat(response.description()).isNull();
        assertThat(response.url()).isEqualTo("https://example.edu/nuevo");
        assertThat(response.displayOrder()).isEqualTo(5);
        assertThat(response.active()).isFalse();
        assertThat(link.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void deletesOnlySelectedLink() {
        InterestingLink link = link(3L, "Eliminar", "https://example.edu/delete", 0, true);
        when(interestingLinkRepository.findById(3L)).thenReturn(Optional.of(link));

        service.delete(3L);

        verify(interestingLinkRepository).delete(link);
    }

    @Test
    void rejectsInvalidUrls() {
        assertThatThrownBy(() -> service.create(new AdminInterestingLinkRequest(
                "Recurso",
                null,
                "ftp://example.edu",
                0,
                true)))
                .isInstanceOf(InvalidInterestingLinkRequestException.class)
                .hasMessage("La URL debe usar http o https.");
    }

    @Test
    void rejectsBlankTitleAndNegativeOrder() {
        assertThatThrownBy(() -> service.create(new AdminInterestingLinkRequest(
                "   ",
                null,
                "https://example.edu",
                0,
                true)))
                .isInstanceOf(InvalidInterestingLinkRequestException.class);

        assertThatThrownBy(() -> service.create(new AdminInterestingLinkRequest(
                "Recurso",
                null,
                "https://example.edu",
                -1,
                true)))
                .isInstanceOf(InvalidInterestingLinkRequestException.class);
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingLink() {
        when(interestingLinkRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, new AdminInterestingLinkRequest(
                "Recurso",
                null,
                "https://example.edu",
                0,
                true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private InterestingLink link(Long id, String title, String url, int displayOrder, boolean active) {
        return new InterestingLink(
                id,
                title,
                "Descripcion",
                url,
                displayOrder,
                active,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"));
    }
}
