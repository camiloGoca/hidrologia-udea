package edu.udea.hidrologia.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.dto.AdminTagResponse;
import edu.udea.hidrologia.tag.dto.UpsertTagRequest;
import edu.udea.hidrologia.tag.entity.Tag;
import edu.udea.hidrologia.tag.repository.TagRepository;
import edu.udea.hidrologia.tag.repository.TagUsageProjection;

@ExtendWith(MockitoExtension.class)
class AdminTagServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private TagRepository tagRepository;

    private AdminTagService adminTagService;

    @BeforeEach
    void setUp() {
        adminTagService = new AdminTagService(
                tagRepository,
                new SlugGenerator(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listsTagsOrderedByRepositoryWithUsageCount() {
        when(tagRepository.findAllWithUsageCount()).thenReturn(List.of(
                projection(2L, "Balance hídrico", "balance-hidrico", 0),
                projection(1L, "Morfometría", "morfometria", 3)));

        List<AdminTagResponse> response = adminTagService.findAll();

        assertThat(response)
                .extracting(AdminTagResponse::slug)
                .containsExactly("balance-hidrico", "morfometria");
        assertThat(response.get(0).usageCount()).isZero();
        assertThat(response.get(1).usageCount()).isEqualTo(3);
    }

    @Test
    void createsTagWithTrimmedNameGeneratedSlugAndZeroUsage() {
        when(tagRepository.existsByNameIgnoreCase("Morfometría")).thenReturn(false);
        when(tagRepository.existsBySlug("morfometria")).thenReturn(false);
        when(tagRepository.saveAndFlush(org.mockito.Mockito.any(Tag.class)))
                .thenAnswer(invocation -> new Tag(1L, "Morfometría", "morfometria", NOW));

        AdminTagResponse response = adminTagService.create(new UpsertTagRequest("  #Morfometría  "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Morfometría");
        assertThat(response.slug()).isEqualTo("morfometria");
        assertThat(response.usageCount()).isZero();
    }

    @Test
    void rejectsDuplicateNameCaseInsensitive() {
        when(tagRepository.existsByNameIgnoreCase("Morfometría")).thenReturn(true);

        assertThatThrownBy(() -> adminTagService.create(new UpsertTagRequest("Morfometría")))
                .isInstanceOf(TagConflictException.class)
                .hasMessage("Ya existe un hashtag con ese nombre o URL.");
    }

    @Test
    void rejectsSlugCollision() {
        when(tagRepository.existsByNameIgnoreCase("Morfometría")).thenReturn(false);
        when(tagRepository.existsBySlug("morfometria")).thenReturn(true);

        assertThatThrownBy(() -> adminTagService.create(new UpsertTagRequest("Morfometría")))
                .isInstanceOf(TagConflictException.class);
    }

    @Test
    void rejectsBlankOrOnlyHashName() {
        assertThatThrownBy(() -> adminTagService.create(new UpsertTagRequest("   ")))
                .isInstanceOf(InvalidTagRequestException.class);
        assertThatThrownBy(() -> adminTagService.create(new UpsertTagRequest(" #  ")))
                .isInstanceOf(InvalidTagRequestException.class);
    }

    @Test
    void renamesTagAndKeepsSlugImmutable() {
        Tag tag = new Tag(1L, "Morfometría", "morfometria", NOW);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameIgnoreCaseAndIdNot("Morfometría de cuencas", 1L)).thenReturn(false);
        when(tagRepository.countUsageById(1L)).thenReturn(2L);

        AdminTagResponse response = adminTagService.rename(1L, new UpsertTagRequest(" Morfometría de cuencas "));

        assertThat(response.name()).isEqualTo("Morfometría de cuencas");
        assertThat(response.slug()).isEqualTo("morfometria");
        assertThat(response.usageCount()).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateRename() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(new Tag(1L, "Cuencas", "cuencas", NOW)));
        when(tagRepository.existsByNameIgnoreCaseAndIdNot("Morfometría", 1L)).thenReturn(true);

        assertThatThrownBy(() -> adminTagService.rename(1L, new UpsertTagRequest("Morfometría")))
                .isInstanceOf(TagConflictException.class);
    }

    @Test
    void returnsNotFoundWhenRenameTargetDoesNotExist() {
        when(tagRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTagService.rename(404L, new UpsertTagRequest("Morfometría")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tag not found");
    }

    @Test
    void deletesUnusedTag() {
        Tag tag = new Tag(1L, "Cuencas", "cuencas", NOW);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.countUsageById(1L)).thenReturn(0L);

        adminTagService.delete(1L);

        verify(tagRepository).delete(tag);
    }

    @Test
    void rejectsDeletingTagUsedByAnyPostStatus() {
        Tag tag = new Tag(1L, "Cuencas", "cuencas", NOW);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.countUsageById(1L)).thenReturn(1L);

        assertThatThrownBy(() -> adminTagService.delete(1L))
                .isInstanceOf(TagConflictException.class)
                .hasMessage("El hashtag está siendo usado por publicaciones. Quítalo de ellas antes de eliminarlo.");
        verify(tagRepository, never()).delete(tag);
    }

    @Test
    void returnsNotFoundWhenDeleteTargetDoesNotExist() {
        when(tagRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTagService.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tag not found");
    }

    private TagUsageProjection projection(Long id, String name, String slug, long usageCount) {
        return new TagUsageProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSlug() {
                return slug;
            }

            @Override
            public long getUsageCount() {
                return usageCount;
            }
        };
    }
}
