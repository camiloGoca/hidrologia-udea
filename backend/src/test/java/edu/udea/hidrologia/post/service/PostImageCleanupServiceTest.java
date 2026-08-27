package edu.udea.hidrologia.post.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageService;

@ExtendWith(MockitoExtension.class)
class PostImageCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    @Mock
    private ImageStorageService imageStorageService;

    private PostImageCleanupService service;

    @BeforeEach
    void setUp() {
        service = new PostImageCleanupService(postImageRepository, imageStorageServiceProvider);
    }

    @Test
    void skipsStorageWhenPostHasNoImages() {
        when(postImageRepository.findByPostIdOrderById(9L)).thenReturn(List.of());

        service.deleteAllForPost(9L);

        verifyNoInteractions(imageStorageServiceProvider, imageStorageService);
        verify(postImageRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void deletesRemoteImagesBeforeMetadata() {
        PostImage image = postImage(15L, "hidrologia-udea/posts/9/post-9-image-1");
        List<PostImage> images = List.of(image);
        when(postImageRepository.findByPostIdOrderById(9L)).thenReturn(images);
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(image.getPublicId())).thenReturn(ImageDeletionResult.DELETED);

        service.deleteAllForPost(9L);

        InOrder inOrder = inOrder(imageStorageService, postImageRepository);
        inOrder.verify(imageStorageService).delete(image.getPublicId());
        inOrder.verify(postImageRepository).deleteAll(images);
        inOrder.verify(postImageRepository).flush();
    }

    @Test
    void treatsMissingRemoteImageAsIdempotentCleanup() {
        PostImage image = postImage(15L, "hidrologia-udea/posts/9/post-9-image-1");
        List<PostImage> images = List.of(image);
        when(postImageRepository.findByPostIdOrderById(9L)).thenReturn(images);
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(image.getPublicId())).thenReturn(ImageDeletionResult.NOT_FOUND);

        service.deleteAllForPost(9L);

        verify(postImageRepository).deleteAll(images);
        verify(postImageRepository).flush();
    }

    @Test
    void preservesMetadataWhenRemoteDeleteFails() {
        PostImage image = postImage(15L, "hidrologia-udea/posts/9/post-9-image-1");
        List<PostImage> images = List.of(image);
        when(postImageRepository.findByPostIdOrderById(9L)).thenReturn(images);
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(image.getPublicId()))
                .thenThrow(new ImageStorageException("delete failed", new RuntimeException()));

        assertThatThrownBy(() -> service.deleteAllForPost(9L))
                .isInstanceOf(PostStateConflictException.class)
                .hasMessage("Post images could not be deleted. Try again.");

        verify(postImageRepository, never()).deleteAll(images);
        verify(postImageRepository, never()).flush();
    }

    private PostImage postImage(Long id, String publicId) {
        Post post = new Post(
                9L,
                new Section(1L, SectionType.TALLER, "Taller 1", "taller-1", "Morfometria", 1, true, NOW),
                "",
                "",
                PostStatus.DRAFT,
                NOW,
                NOW,
                null,
                Set.of(),
                null);

        return new PostImage(
                id,
                post,
                publicId,
                "https://res.cloudinary.com/demo/image/upload/post.png",
                "png",
                800,
                600,
                1000L,
                "Grafica",
                NOW);
    }
}
