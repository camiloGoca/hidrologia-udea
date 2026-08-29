package edu.udea.hidrologia.shared.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.udea.hidrologia.shared.storage.ImageUpload;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageRequest;
import edu.udea.hidrologia.shared.storage.StoredImage;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryImageStorageService(cloudinary);
    }

    @Test
    void uploadsImageAndMapsCloudinaryMetadata() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "public_id", "hidrologia-udea/questions/question-1",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/question-1.png",
                "format", "png",
                "width", 1200,
                "height", 800,
                "bytes", 12345));

        StoredImage storedImage = service.upload(new ImageUpload(new byte[] {1, 2, 3}, "png", 2, 2, 3));

        assertThat(storedImage.publicId()).isEqualTo("hidrologia-udea/questions/question-1");
        assertThat(storedImage.secureUrl()).startsWith("https://");
        assertThat(storedImage.format()).isEqualTo("png");
        assertThat(storedImage.width()).isEqualTo(1200);
        assertThat(storedImage.height()).isEqualTo(800);
        assertThat(storedImage.bytes()).isEqualTo(12345);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Object, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), optionsCaptor.capture());

        assertThat(optionsCaptor.getValue()).containsEntry("folder", "hidrologia-udea/questions");
        assertThat(optionsCaptor.getValue()).containsEntry("overwrite", false);
        assertThat(optionsCaptor.getValue()).containsEntry("resource_type", "image");
    }

    @Test
    void uploadsImageToRequestedFolderAndPrefix() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "public_id", "hidrologia-udea/posts/9/post-9-image-1",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/post-9-image-1.png",
                "format", "png",
                "width", 1200,
                "height", 800,
                "bytes", 12345));

        service.upload(
                new ImageUpload(new byte[] {1, 2, 3}, "png", 2, 2, 3),
                new ImageStorageRequest("hidrologia-udea/posts/9", "post-9-image"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Object, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), optionsCaptor.capture());

        assertThat(optionsCaptor.getValue()).containsEntry("folder", "hidrologia-udea/posts/9");
        assertThat(optionsCaptor.getValue().get("public_id").toString()).startsWith("post-9-image-");
    }

    @Test
    void canonicalizesJpegFormatReturnedByCloudinary() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "public_id", "hidrologia-udea/posts/9/post-9-image-1",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/post-9-image-1.jpg",
                "format", "JPEG",
                "width", 1200,
                "height", 800,
                "bytes", 12345));

        StoredImage storedImage = service.upload(
                new ImageUpload(new byte[] {1, 2, 3}, "jpg", 2, 2, 3),
                new ImageStorageRequest("hidrologia-udea/posts/9", "post-9-image"));

        assertThat(storedImage.format()).isEqualTo("jpg");
    }

    @Test
    void rejectsUnsupportedFormatReturnedByCloudinary() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "public_id", "hidrologia-udea/posts/9/post-9-image-1",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/post-9-image-1.webp",
                "format", "webp",
                "width", 1200,
                "height", 800,
                "bytes", 12345));

        assertThatThrownBy(() -> service.upload(
                new ImageUpload(new byte[] {1, 2, 3}, "jpg", 2, 2, 3),
                new ImageStorageRequest("hidrologia-udea/posts/9", "post-9-image")))
                .isInstanceOf(ImageStorageException.class);
    }

    @Test
    void translatesRuntimeFailuresDuringUpload() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap()))
                .thenThrow(new RuntimeException("Request forbidden due to missing permissions"));

        assertThatThrownBy(() -> service.upload(new ImageUpload(new byte[] {1, 2, 3}, "png", 2, 2, 3)))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Image storage is temporarily unavailable")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void deletesImageByPublicId() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy("hidrologia-udea/questions/question-1", Map.of(
                "resource_type",
                "image",
                "invalidate",
                true))).thenReturn(Map.of("result", "ok"));

        ImageDeletionResult result = service.delete("hidrologia-udea/questions/question-1");

        assertThat(result).isEqualTo(ImageDeletionResult.DELETED);
        verify(uploader).destroy("hidrologia-udea/questions/question-1", Map.of(
                "resource_type",
                "image",
                "invalidate",
                true));
    }

    @Test
    void treatsCloudinaryNotFoundAsVerifiedDeletion() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(), anyMap())).thenReturn(Map.of("result", "not found"));

        ImageDeletionResult result = service.delete("hidrologia-udea/posts/9/missing");

        assertThat(result).isEqualTo(ImageDeletionResult.NOT_FOUND);
    }

    @Test
    void translatesRuntimeFailuresDuringDelete() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(), anyMap()))
                .thenThrow(new RuntimeException("Request forbidden due to missing permissions"));

        assertThatThrownBy(() -> service.delete("hidrologia-udea/posts/9/post-9-image"))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Image storage is temporarily unavailable")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
