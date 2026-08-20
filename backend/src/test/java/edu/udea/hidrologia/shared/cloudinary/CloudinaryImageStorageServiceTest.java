package edu.udea.hidrologia.shared.cloudinary;

import static org.assertj.core.api.Assertions.assertThat;
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
    void deletesImageByPublicId() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);

        service.delete("hidrologia-udea/questions/question-1");

        verify(uploader).destroy("hidrologia-udea/questions/question-1", Map.of(
                "resource_type",
                "image",
                "invalidate",
                true));
    }
}
