package edu.udea.hidrologia.post.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.AdminPostImageResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageFileValidator;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.ImageStorageRequest;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;
import edu.udea.hidrologia.shared.storage.ImageUpload;
import edu.udea.hidrologia.shared.storage.InvalidImageException;
import edu.udea.hidrologia.shared.storage.StoredImage;

@Service
public class AdminPostImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPostImageService.class);
    private static final int ALT_TEXT_MAX_LENGTH = 180;
    private static final String POSTS_FOLDER = "hidrologia-udea/posts";

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final ImageFileValidator imageFileValidator;
    private final ObjectProvider<ImageStorageService> imageStorageServiceProvider;
    private final PostContentDocumentService postContentDocumentService;
    private final Clock clock;

    public AdminPostImageService(
            PostRepository postRepository,
            PostImageRepository postImageRepository,
            ImageFileValidator imageFileValidator,
            ObjectProvider<ImageStorageService> imageStorageServiceProvider,
            PostContentDocumentService postContentDocumentService,
            Clock clock) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.imageFileValidator = imageFileValidator;
        this.imageStorageServiceProvider = imageStorageServiceProvider;
        this.postContentDocumentService = postContentDocumentService;
        this.clock = clock;
    }

    public AdminPostImageResponse upload(Long postId, MultipartFile file, String altText) {
        Post post = findPost(postId);
        String normalizedAltText = normalizeAltText(altText);
        ImageUpload imageUpload = imageFileValidator.validateOptional(file)
                .orElseThrow(() -> new InvalidImageException("The uploaded image is required"));
        ImageStorageService imageStorageService = imageStorageService();
        StoredImage storedImage = imageStorageService.upload(
                imageUpload,
                new ImageStorageRequest(POSTS_FOLDER + "/" + post.getId(), "post-" + post.getId() + "-image"));

        try {
            PostImage postImage = postImageRepository.saveAndFlush(new PostImage(
                    null,
                    post,
                    storedImage.publicId(),
                    storedImage.secureUrl(),
                    storedImage.format(),
                    storedImage.width(),
                    storedImage.height(),
                    storedImage.bytes(),
                    normalizedAltText,
                    Instant.now(clock)));

            return toResponse(postImage);
        } catch (RuntimeException exception) {
            compensateUploadedImage(imageStorageService, storedImage.publicId(), exception);
            throw exception;
        }
    }

    public AdminPostImageResponse updateAltText(Long postId, Long imageId, Map<String, Object> request) {
        if (request == null || !request.keySet().equals(java.util.Set.of("altText"))) {
            throw new InvalidPostDraftRequestException("Post image update request is invalid");
        }

        Object value = request.get("altText");
        if (!(value instanceof String altText)) {
            throw new InvalidPostDraftRequestException("Post image update request is invalid");
        }

        PostImage postImage = findPostImage(postId, imageId);
        postImage.updateAltText(normalizeAltText(altText));

        return toResponse(postImageRepository.saveAndFlush(postImage));
    }

    public void delete(Long postId, Long imageId) {
        PostImage postImage = findPostImage(postId, imageId);
        if (postContentDocumentService.referencesPostImageId(postImage.getPost().getContentDocument(), imageId)) {
            throw new PostStateConflictException("Remove the image from the post content before deleting it");
        }

        ImageStorageService imageStorageService = imageStorageService();
        ImageDeletionResult result = imageStorageService.delete(postImage.getPublicId());
        if (result != ImageDeletionResult.DELETED && result != ImageDeletionResult.NOT_FOUND) {
            throw new ImageStorageException(
                    "Image storage deletion could not be verified",
                    new IllegalStateException("Unexpected image deletion result"));
        }

        postImageRepository.delete(postImage);
    }

    private Post findPost(Long postId) {
        return postRepository.findAdminById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private PostImage findPostImage(Long postId, Long imageId) {
        return postImageRepository.findByIdAndPostId(imageId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post image not found"));
    }

    private ImageStorageService imageStorageService() {
        ImageStorageService imageStorageService = imageStorageServiceProvider.getIfAvailable();
        if (imageStorageService == null) {
            throw new ImageStorageUnavailableException("Image uploads are temporarily unavailable");
        }

        return imageStorageService;
    }

    private String normalizeAltText(String value) {
        if (value == null) {
            throw new InvalidPostDraftRequestException("Post image alt text is required");
        }

        String normalized = value.strip();
        if (normalized.isBlank() || normalized.length() > ALT_TEXT_MAX_LENGTH) {
            throw new InvalidPostDraftRequestException("Post image alt text is invalid");
        }

        return normalized;
    }

    private void compensateUploadedImage(
            ImageStorageService imageStorageService,
            String publicId,
            RuntimeException originalException) {
        try {
            imageStorageService.delete(publicId);
        } catch (RuntimeException deleteException) {
            originalException.addSuppressed(deleteException);
            LOGGER.error("Failed to delete uploaded post image after persistence failure", deleteException);
        }
    }

    AdminPostImageResponse toResponse(PostImage postImage) {
        return new AdminPostImageResponse(
                postImage.getId(),
                postImage.getSecureUrl(),
                postImage.getFormat(),
                postImage.getWidth(),
                postImage.getHeight(),
                postImage.getBytes(),
                postImage.getAltText(),
                postImage.getCreatedAt());
    }
}
