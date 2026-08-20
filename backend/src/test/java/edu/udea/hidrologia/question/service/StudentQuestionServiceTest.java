package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.shared.storage.ImageFileValidator;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;
import edu.udea.hidrologia.shared.storage.ImageUpload;
import edu.udea.hidrologia.shared.storage.ImageUploadProperties;
import edu.udea.hidrologia.shared.storage.StoredImage;

@ExtendWith(MockitoExtension.class)
class StudentQuestionServiceTest {

    private static final CreateStudentQuestionRequest REQUEST =
            new CreateStudentQuestionRequest("taller-1", "Estudiante", "Pregunta de prueba");
    private static final CreateStudentQuestionResponse RESPONSE =
            new CreateStudentQuestionResponse(1L, StudentQuestionStatus.PENDING, Instant.parse("2026-01-01T00:00:00Z"));
    private static final StoredImage STORED_IMAGE =
            new StoredImage("hidrologia-udea/questions/question-1", "https://example.com/image.png", "png", 2, 2, 79);

    @Mock
    private StudentQuestionPersistenceService persistenceService;

    @Mock
    private ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    @Mock
    private ImageStorageService imageStorageService;

    private StudentQuestionService studentQuestionService;

    @BeforeEach
    void setUp() {
        ImageUploadProperties properties = new ImageUploadProperties();
        studentQuestionService = new StudentQuestionService(
                persistenceService,
                new ImageFileValidator(properties),
                imageStorageServiceProvider);
    }

    @Test
    void createsQuestionWithoutImageWhenCloudinaryIsDisabled() {
        when(persistenceService.persist(REQUEST, null)).thenReturn(RESPONSE);

        studentQuestionService.createQuestion(REQUEST, null);

        verify(persistenceService).persist(REQUEST, null);
        verifyNoInteractions(imageStorageServiceProvider, imageStorageService);
    }

    @Test
    void uploadsValidJpegImageOnceAndPersistsMetadata() throws Exception {
        MockMultipartFile image = image("image.jpg", "image/jpeg", "jpg");
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class))).thenReturn(STORED_IMAGE);
        when(persistenceService.persist(REQUEST, STORED_IMAGE)).thenReturn(RESPONSE);

        studentQuestionService.createQuestion(REQUEST, image);

        verify(imageStorageService).upload(any(ImageUpload.class));
        verify(persistenceService).persist(REQUEST, STORED_IMAGE);
        verify(imageStorageService, never()).delete(any());
    }

    @Test
    void uploadsValidPngImageOnceAndPersistsMetadata() throws Exception {
        MockMultipartFile image = image("image.png", "image/png", "png");
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class))).thenReturn(STORED_IMAGE);
        when(persistenceService.persist(REQUEST, STORED_IMAGE)).thenReturn(RESPONSE);

        studentQuestionService.createQuestion(REQUEST, image);

        verify(imageStorageService).upload(any(ImageUpload.class));
        verify(persistenceService).persist(REQUEST, STORED_IMAGE);
    }

    @Test
    void rejectsImageWhenCloudinaryIsDisabled() throws Exception {
        MockMultipartFile image = image("image.png", "image/png", "png");
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> studentQuestionService.createQuestion(REQUEST, image))
                .isInstanceOf(ImageStorageUnavailableException.class)
                .hasMessage("Image uploads are temporarily unavailable");

        verifyNoInteractions(imageStorageService);
        verify(persistenceService, never()).persist(any(), any());
    }

    @Test
    void deletesUploadedImageWhenPersistenceFails() throws Exception {
        MockMultipartFile image = image("image.png", "image/png", "png");
        RuntimeException dbFailure = new RuntimeException("db failure");
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class))).thenReturn(STORED_IMAGE);
        when(persistenceService.persist(REQUEST, STORED_IMAGE)).thenThrow(dbFailure);

        assertThatThrownBy(() -> studentQuestionService.createQuestion(REQUEST, image))
                .isSameAs(dbFailure);

        verify(imageStorageService).delete(STORED_IMAGE.publicId());
    }

    @Test
    void deleteCompensationFailureDoesNotHideOriginalPersistenceFailure() throws Exception {
        MockMultipartFile image = image("image.png", "image/png", "png");
        RuntimeException dbFailure = new RuntimeException("db failure");
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class))).thenReturn(STORED_IMAGE);
        when(persistenceService.persist(REQUEST, STORED_IMAGE)).thenThrow(dbFailure);
        org.mockito.Mockito.doThrow(new ImageStorageException("delete failed", new RuntimeException()))
                .when(imageStorageService)
                .delete(STORED_IMAGE.publicId());

        assertThatThrownBy(() -> studentQuestionService.createQuestion(REQUEST, image))
                .isSameAs(dbFailure);
    }

    @Test
    void doesNotPersistQuestionWhenImageUploadFails() throws Exception {
        MockMultipartFile image = image("image.png", "image/png", "png");
        ImageStorageException uploadFailure = new ImageStorageException("upload failed", new RuntimeException());
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.upload(any(ImageUpload.class))).thenThrow(uploadFailure);

        assertThatThrownBy(() -> studentQuestionService.createQuestion(REQUEST, image))
                .isSameAs(uploadFailure);

        verify(persistenceService, never()).persist(any(), any());
        verify(imageStorageService, never()).delete(any());
    }

    private MockMultipartFile image(String filename, String contentType, String format) throws Exception {
        BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, format, output);

        return new MockMultipartFile("image", filename, contentType, output.toByteArray());
    }
}
