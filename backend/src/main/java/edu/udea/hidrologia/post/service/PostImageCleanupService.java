package edu.udea.hidrologia.post.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.shared.storage.ImageDeletionResult;
import edu.udea.hidrologia.shared.storage.ImageStorageService;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;

@Service
public class PostImageCleanupService {

    private static final String CLEANUP_FAILURE_MESSAGE = "Post images could not be deleted. Try again.";

    private final PostImageRepository postImageRepository;
    private final ObjectProvider<ImageStorageService> imageStorageServiceProvider;

    public PostImageCleanupService(
            PostImageRepository postImageRepository,
            ObjectProvider<ImageStorageService> imageStorageServiceProvider) {
        this.postImageRepository = postImageRepository;
        this.imageStorageServiceProvider = imageStorageServiceProvider;
    }

    public void deleteAllForPost(Long postId) {
        List<PostImage> images = postImageRepository.findByPostIdOrderById(postId);
        if (images.isEmpty()) {
            return;
        }

        ImageStorageService imageStorageService = imageStorageService();
        for (PostImage image : images) {
            deleteRemoteImage(imageStorageService, image);
        }

        postImageRepository.deleteAll(images);
        postImageRepository.flush();
    }

    private void deleteRemoteImage(ImageStorageService imageStorageService, PostImage image) {
        ImageDeletionResult result;
        try {
            result = imageStorageService.delete(image.getPublicId());
        } catch (RuntimeException exception) {
            throw new PostStateConflictException(CLEANUP_FAILURE_MESSAGE);
        }

        if (result != ImageDeletionResult.DELETED && result != ImageDeletionResult.NOT_FOUND) {
            throw new PostStateConflictException(CLEANUP_FAILURE_MESSAGE);
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
