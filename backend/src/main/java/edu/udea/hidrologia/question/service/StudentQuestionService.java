package edu.udea.hidrologia.question.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.shared.storage.ImageFileValidator;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;
import edu.udea.hidrologia.shared.storage.ImageUpload;
import edu.udea.hidrologia.shared.storage.StoredImage;

@Service
public class StudentQuestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentQuestionService.class);

    private final StudentQuestionPersistenceService persistenceService;
    private final ImageFileValidator imageFileValidator;
    private final ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    public StudentQuestionService(
            StudentQuestionPersistenceService persistenceService,
            ImageFileValidator imageFileValidator,
            ObjectProvider<ImageStorageService> imageStorageServiceProvider) {
        this.persistenceService = persistenceService;
        this.imageFileValidator = imageFileValidator;
        this.imageStorageServiceProvider = imageStorageServiceProvider;
    }

    public CreateStudentQuestionResponse createQuestion(CreateStudentQuestionRequest request, MultipartFile image) {
        Optional<ImageUpload> imageUpload = imageFileValidator.validateOptional(image);

        if (imageUpload.isEmpty()) {
            return persistenceService.persist(request, null);
        }

        ImageStorageService imageStorageService = imageStorageServiceProvider.getIfAvailable();
        if (imageStorageService == null) {
            throw new ImageStorageUnavailableException("Image uploads are temporarily unavailable");
        }

        StoredImage storedImage = imageStorageService.upload(imageUpload.get());

        try {
            return persistenceService.persist(request, storedImage);
        } catch (RuntimeException exception) {
            compensateUploadedImage(imageStorageService, storedImage.publicId(), exception);
            throw exception;
        }
    }

    private void compensateUploadedImage(
            ImageStorageService imageStorageService,
            String publicId,
            RuntimeException originalException) {
        try {
            imageStorageService.delete(publicId);
        } catch (RuntimeException deleteException) {
            LOGGER.warn("Failed to delete uploaded question image after persistence failure. publicId={}", publicId,
                    deleteException);
        }
    }
}
