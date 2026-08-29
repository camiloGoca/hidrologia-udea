package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;

@ExtendWith(MockitoExtension.class)
class QuestionAttachmentCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    @Mock
    private ImageStorageService imageStorageService;

    private QuestionAttachmentCleanupService service;

    @BeforeEach
    void setUp() {
        service = new QuestionAttachmentCleanupService(imageStorageServiceProvider);
    }

    @Test
    void skipsStorageWhenAttachmentIsNull() {
        service.deleteRemoteAttachment(null);

        verify(imageStorageServiceProvider, never()).getIfAvailable();
    }

    @Test
    void treatsDeletedRemoteAttachmentAsSuccess() {
        QuestionAttachment attachment = attachment();
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(attachment.getPublicId())).thenReturn(ImageDeletionResult.DELETED);

        service.deleteRemoteAttachment(attachment);

        verify(imageStorageService).delete(attachment.getPublicId());
    }

    @Test
    void treatsMissingRemoteAttachmentAsIdempotentSuccess() {
        QuestionAttachment attachment = attachment();
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(attachment.getPublicId())).thenReturn(ImageDeletionResult.NOT_FOUND);

        service.deleteRemoteAttachment(attachment);

        verify(imageStorageService).delete(attachment.getPublicId());
    }

    @Test
    void failsClosedWhenStorageIsUnavailable() {
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.deleteRemoteAttachment(attachment()))
                .isInstanceOf(ImageStorageUnavailableException.class);
    }

    @Test
    void wrapsUnexpectedStorageRuntimeFailures() {
        QuestionAttachment attachment = attachment();
        when(imageStorageServiceProvider.getIfAvailable()).thenReturn(imageStorageService);
        when(imageStorageService.delete(attachment.getPublicId())).thenThrow(new RuntimeException("sdk failed"));

        assertThatThrownBy(() -> service.deleteRemoteAttachment(attachment))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Question attachment could not be deleted");
    }

    private QuestionAttachment attachment() {
        return new QuestionAttachment(
                1L,
                null,
                "hidrologia-udea/questions/private-id",
                "https://res.cloudinary.com/demo/image/upload/question.png",
                "png",
                640,
                480,
                1000L,
                NOW);
    }
}
