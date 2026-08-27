package edu.udea.hidrologia.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.json.JsonMapper;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.AdminPostImageResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageFileValidator;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageRequest;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageTooLargeException;
import edu.udea.hidrologia.shared.storage.ImageUpload;
import edu.udea.hidrologia.shared.storage.ImageUploadProperties;
import edu.udea.hidrologia.shared.storage.InvalidImageException;
import edu.udea.hidrologia.shared.storage.StoredImage;

@ExtendWith(MockitoExtension.class)
class AdminPostImageServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-03T00:00:00Z");
    private static final StoredImage STORED_IMAGE = new StoredImage(
            "hidrologia-udea/posts/9/post-9-image-1",
            "https://res.cloudinary.com/demo/image/upload/post-9-image-1.png",
            "png",
            800,
            600,
            1000L);

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    @Mock
    private ImageStorageService imageStorageService;

    private AdminPostImageService service;

    @BeforeEach
    void setUp() {
        ImageUploadProperties properties = new ImageUploadProperties();
        service = new AdminPostImageService(
                postRepository,
                postImageRepository,
                new ImageFileValidator(properties),
                imageStorageServiceProvider,
                new PostContentDocumentService(JsonMapper.builder().build()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void uploadsValidJpegImageAndPersistsPostImageMetadata() throws Exception {
        Post post = post(PostStatus.PUBLISHED, documentWithoutImage());
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class), any(ImageStorageRequest.class))).thenReturn(STORED_IMAGE);
        when(postImageRepository.saveAndFlush(any(PostImage.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 15L));

        AdminPostImageResponse response = service.upload(9L, image("file", "image.jpg", "image/jpeg", "jpg"),
                "  Diagrama de cuenca  ");

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.secureUrl()).isEqualTo(STORED_IMAGE.secureUrl());
        assertThat(response.format()).isEqualTo("png");
        assertThat(response.width()).isEqualTo(800);
        assertThat(response.height()).isEqualTo(600);
        assertThat(response.bytes()).isEqualTo(1000L);
        assertThat(response.altText()).isEqualTo("Diagrama de cuenca");
        assertThat(response.createdAt()).isEqualTo(NOW);

        ArgumentCaptor<ImageUpload> uploadCaptor = ArgumentCaptor.forClass(ImageUpload.class);
        ArgumentCaptor<ImageStorageRequest> requestCaptor = ArgumentCaptor.forClass(ImageStorageRequest.class);
        verify(imageStorageService).upload(uploadCaptor.capture(), requestCaptor.capture());
        assertThat(uploadCaptor.getValue().format()).isEqualTo("jpg");
        assertThat(requestCaptor.getValue().folder()).isEqualTo("hidrologia-udea/posts/9");
        assertThat(requestCaptor.getValue().publicIdPrefix()).isEqualTo("post-9-image");

        ArgumentCaptor<PostImage> imageCaptor = ArgumentCaptor.forClass(PostImage.class);
        verify(postImageRepository).saveAndFlush(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getPost()).isSameAs(post);
        assertThat(imageCaptor.getValue().getPublicId()).isEqualTo(STORED_IMAGE.publicId());
        assertThat(imageCaptor.getValue().getAltText()).isEqualTo("Diagrama de cuenca");
    }

    @Test
    void uploadsValidPngImage() throws Exception {
        Post post = post(PostStatus.DRAFT, documentWithoutImage());
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class), any(ImageStorageRequest.class))).thenReturn(STORED_IMAGE);
        when(postImageRepository.saveAndFlush(any(PostImage.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 15L));

        service.upload(9L, image("file", "image.png", "image/png", "png"), "Gráfica");

        verify(postImageRepository).saveAndFlush(any(PostImage.class));
    }

    @Test
    void rejectsSpoofedImageContent() throws Exception {
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post(PostStatus.DRAFT, documentWithoutImage())));

        assertThatThrownBy(() -> service.upload(9L, image("file", "fake.png", "image/png", "gif"), "Imagen"))
                .isInstanceOf(InvalidImageException.class);

        verifyNoInteractions(imageStorageServiceProvider, imageStorageService, postImageRepository);
    }

    @Test
    void rejectsImageLargerThanFiveMb() {
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post(PostStatus.DRAFT, documentWithoutImage())));
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                new byte[(int) (5L * 1024L * 1024L + 1L)]);

        assertThatThrownBy(() -> service.upload(9L, largeFile, "Imagen"))
                .isInstanceOf(ImageTooLargeException.class);
    }

    @Test
    void rejectsBlankAltText() throws Exception {
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post(PostStatus.DRAFT, documentWithoutImage())));

        assertThatThrownBy(() -> service.upload(9L, image("file", "image.png", "image/png", "png"), "   "))
                .isInstanceOf(InvalidPostDraftRequestException.class);

        verifyNoInteractions(imageStorageServiceProvider, imageStorageService, postImageRepository);
    }

    @Test
    void rejectsTooLongAltText() throws Exception {
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post(PostStatus.DRAFT, documentWithoutImage())));

        assertThatThrownBy(() -> service.upload(9L, image("file", "image.png", "image/png", "png"), "a".repeat(181)))
                .isInstanceOf(InvalidPostDraftRequestException.class);
    }

    @Test
    void throwsNotFoundWhenPostDoesNotExist() throws Exception {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(404L, image("file", "image.png", "image/png", "png"), "Imagen"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
    }

    @Test
    void deletesUploadedImageWhenPersistenceFails() throws Exception {
        RuntimeException dbFailure = new RuntimeException("db failed");
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post(PostStatus.DRAFT, documentWithoutImage())));
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class), any(ImageStorageRequest.class))).thenReturn(STORED_IMAGE);
        when(postImageRepository.saveAndFlush(any(PostImage.class))).thenThrow(dbFailure);

        assertThatThrownBy(() -> service.upload(9L, image("file", "image.png", "image/png", "png"), "Imagen"))
                .isSameAs(dbFailure);

        verify(imageStorageService).delete(STORED_IMAGE.publicId());
    }

    @Test
    void updatesAltTextOnlyWhenImageBelongsToPost() {
        PostImage postImage = postImage(15L, post(PostStatus.PUBLISHED, documentWithoutImage()));
        when(postImageRepository.findByIdAndPostId(15L, 9L)).thenReturn(Optional.of(postImage));
        when(postImageRepository.saveAndFlush(postImage)).thenReturn(postImage);

        AdminPostImageResponse response = service.updateAltText(9L, 15L, Map.of("altText", "  Nueva descripción  "));

        assertThat(response.altText()).isEqualTo("Nueva descripción");
    }

    @Test
    void rejectsPatchWhenImageDoesNotBelongToPost() {
        when(postImageRepository.findByIdAndPostId(15L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAltText(9L, 15L, Map.of("altText", "Nueva descripción")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post image not found");
    }

    @Test
    void rejectsPatchWithUnsupportedFields() {
        assertThatThrownBy(() -> service.updateAltText(9L, 15L, Map.of("altText", "Imagen", "publicId", "secret")))
                .isInstanceOf(InvalidPostDraftRequestException.class);
    }

    @Test
    void rejectsDeleteWhenContentDocumentReferencesImage() {
        PostImage postImage = postImage(15L, post(PostStatus.PUBLISHED, documentWithImage(15L)));
        when(postImageRepository.findByIdAndPostId(15L, 9L)).thenReturn(Optional.of(postImage));

        assertThatThrownBy(() -> service.delete(9L, 15L))
                .isInstanceOf(PostStateConflictException.class)
                .hasMessage("Remove the image from the post content before deleting it");

        verifyNoInteractions(imageStorageServiceProvider, imageStorageService);
        verify(postImageRepository, never()).delete(postImage);
    }

    @Test
    void deletesCloudinaryAssetBeforeMetadataWhenImageIsNotReferenced() {
        PostImage postImage = postImage(15L, post(PostStatus.PUBLISHED, documentWithoutImage()));
        when(postImageRepository.findByIdAndPostId(15L, 9L)).thenReturn(Optional.of(postImage));
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(postImage.getPublicId())).thenReturn(ImageDeletionResult.DELETED);

        service.delete(9L, 15L);

        verify(imageStorageService).delete(postImage.getPublicId());
        verify(postImageRepository).delete(postImage);
    }

    @Test
    void treatsMissingCloudinaryAssetAsIdempotentDelete() {
        PostImage postImage = postImage(15L, post(PostStatus.PUBLISHED, documentWithoutImage()));
        when(postImageRepository.findByIdAndPostId(15L, 9L)).thenReturn(Optional.of(postImage));
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(postImage.getPublicId())).thenReturn(ImageDeletionResult.NOT_FOUND);

        service.delete(9L, 15L);

        verify(postImageRepository).delete(postImage);
    }

    @Test
    void preservesDbRowWhenCloudinaryDeleteFails() {
        PostImage postImage = postImage(15L, post(PostStatus.PUBLISHED, documentWithoutImage()));
        when(postImageRepository.findByIdAndPostId(15L, 9L)).thenReturn(Optional.of(postImage));
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(postImage.getPublicId()))
                .thenThrow(new ImageStorageException("delete failed", new RuntimeException()));

        assertThatThrownBy(() -> service.delete(9L, 15L))
                .isInstanceOf(ImageStorageException.class);

        verify(postImageRepository, never()).delete(postImage);
    }

    private MockMultipartFile image(String partName, String filename, String contentType, String format) throws Exception {
        BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, format, output);

        return new MockMultipartFile(partName, filename, contentType, output.toByteArray());
    }

    private Post post(PostStatus status, Map<String, Object> contentDocument) {
        Instant publishedAt = status == PostStatus.PUBLISHED || status == PostStatus.ARCHIVED ? NOW : null;
        String title = status == PostStatus.DRAFT ? "" : "Título";
        String content = status == PostStatus.DRAFT ? "" : "Contenido";

        return new Post(
                9L,
                new Section(1L, SectionType.TALLER, "Taller 1", "taller-1", "Morfometría de cuencas", 1, true, NOW),
                title,
                content,
                contentDocument,
                status,
                NOW,
                NOW,
                publishedAt,
                Set.of(),
                null);
    }

    private PostImage postImage(Long id, Post post) {
        return new PostImage(
                id,
                post,
                STORED_IMAGE.publicId(),
                STORED_IMAGE.secureUrl(),
                STORED_IMAGE.format(),
                STORED_IMAGE.width(),
                STORED_IMAGE.height(),
                STORED_IMAGE.bytes(),
                "Imagen",
                NOW);
    }

    private PostImage withId(PostImage postImage, Long id) {
        ReflectionTestUtils.setField(postImage, "id", id);

        return postImage;
    }

    private Map<String, Object> documentWithoutImage() {
        return Map.of(
                "type", "doc",
                "content", java.util.List.of(Map.of("type", "paragraph")));
    }

    private Map<String, Object> documentWithImage(Long imageId) {
        return Map.of(
                "type", "doc",
                "content", java.util.List.of(Map.of(
                        "type", "image",
                        "attrs", Map.of("postImageId", imageId))));
    }
}
