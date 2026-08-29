package edu.udea.hidrologia.question.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;

@Service
public class QuestionAttachmentCleanupService {

    private final ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    public QuestionAttachmentCleanupService(ObjectProvider<ImageStorageService> imageStorageServiceProvider) {
        this.imageStorageServiceProvider = imageStorageServiceProvider;
    }

    public void deleteRemoteAttachment(QuestionAttachment attachment) {
        if (attachment == null) {
            return;
        }

        ImageDeletionResult result;
        try {
            result = imageStorageService().delete(attachment.getPublicId());
        } catch (ImageStorageUnavailableException | ImageStorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ImageStorageException("Question attachment could not be deleted", exception);
        }
        if (result != ImageDeletionResult.DELETED && result != ImageDeletionResult.NOT_FOUND) {
            throw new ImageStorageException("Question attachment could not be deleted", null);
        }
    }

    private ImageStorageService imageStorageService() {
        ImageStorageService imageStorageService = imageStorageServiceProvider.getIfAvailable();
        if (imageStorageService == null) {
            throw new ImageStorageUnavailableException("Image uploads are temporarily unavailable");
        }

        return imageStorageService;
    }
}
