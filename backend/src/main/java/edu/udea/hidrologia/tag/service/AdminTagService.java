package edu.udea.hidrologia.tag.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.dto.AdminTagResponse;
import edu.udea.hidrologia.tag.dto.UpsertTagRequest;
import edu.udea.hidrologia.tag.entity.Tag;
import edu.udea.hidrologia.tag.repository.TagRepository;
import edu.udea.hidrologia.tag.repository.TagUsageProjection;
import jakarta.validation.Valid;

@Service
@Validated
public class AdminTagService {

    private static final String DUPLICATE_MESSAGE = "Ya existe un hashtag con ese nombre o URL.";
    private static final String USED_MESSAGE =
            "El hashtag está siendo usado por publicaciones. Quítalo de ellas antes de eliminarlo.";

    private final TagRepository tagRepository;
    private final SlugGenerator slugGenerator;
    private final Clock clock;

    public AdminTagService(TagRepository tagRepository, SlugGenerator slugGenerator, Clock clock) {
        this.tagRepository = tagRepository;
        this.slugGenerator = slugGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminTagResponse> findAll() {
        return tagRepository.findAllWithUsageCount().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminTagResponse create(@Valid UpsertTagRequest request) {
        String name = normalizeName(request.name());
        String slug = slugGenerator.generate(name);
        validateGeneratedSlug(slug);

        if (tagRepository.existsByNameIgnoreCase(name) || tagRepository.existsBySlug(slug)) {
            throw new TagConflictException(DUPLICATE_MESSAGE);
        }

        try {
            Tag tag = tagRepository.saveAndFlush(new Tag(null, name, slug, Instant.now(clock)));
            return new AdminTagResponse(tag.getId(), tag.getName(), tag.getSlug(), 0);
        } catch (DataIntegrityViolationException exception) {
            throw new TagConflictException(DUPLICATE_MESSAGE);
        }
    }

    @Transactional
    public AdminTagResponse rename(Long id, @Valid UpsertTagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        String name = normalizeName(request.name());

        if (tagRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new TagConflictException(DUPLICATE_MESSAGE);
        }

        try {
            tag.rename(name);
            tagRepository.flush();
            return new AdminTagResponse(tag.getId(), tag.getName(), tag.getSlug(), tagRepository.countUsageById(id));
        } catch (DataIntegrityViolationException exception) {
            throw new TagConflictException(DUPLICATE_MESSAGE);
        }
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

        if (tagRepository.countUsageById(id) > 0) {
            throw new TagConflictException(USED_MESSAGE);
        }

        tagRepository.delete(tag);
    }

    private AdminTagResponse toResponse(TagUsageProjection projection) {
        return new AdminTagResponse(
                projection.getId(),
                projection.getName(),
                projection.getSlug(),
                projection.getUsageCount());
    }

    private String normalizeName(String value) {
        String normalized = value.strip();
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1).strip();
        }

        if (normalized.isBlank() || normalized.replace("#", "").isBlank()) {
            throw new InvalidTagRequestException("Escribe el nombre del hashtag.");
        }

        if (normalized.length() > 80) {
            throw new InvalidTagRequestException("El nombre del hashtag no puede superar 80 caracteres.");
        }

        return normalized;
    }

    private void validateGeneratedSlug(String slug) {
        if (slug.isBlank() || slug.length() > 100) {
            throw new InvalidTagRequestException("El nombre del hashtag no genera una URL válida.");
        }
    }
}
