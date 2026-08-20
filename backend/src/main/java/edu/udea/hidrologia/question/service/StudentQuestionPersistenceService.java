package edu.udea.hidrologia.question.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.shared.storage.StoredImage;

@Service
class StudentQuestionPersistenceService {

    private final StudentQuestionRepository studentQuestionRepository;
    private final QuestionAttachmentRepository questionAttachmentRepository;
    private final SectionRepository sectionRepository;
    private final Clock clock;

    StudentQuestionPersistenceService(
            StudentQuestionRepository studentQuestionRepository,
            QuestionAttachmentRepository questionAttachmentRepository,
            SectionRepository sectionRepository,
            Clock clock) {
        this.studentQuestionRepository = studentQuestionRepository;
        this.questionAttachmentRepository = questionAttachmentRepository;
        this.sectionRepository = sectionRepository;
        this.clock = clock;
    }

    @Transactional
    CreateStudentQuestionResponse persist(CreateStudentQuestionRequest request, StoredImage storedImage) {
        Section section = sectionRepository.findBySlugAndActiveTrue(request.sectionSlug().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        Instant now = Instant.now(clock);
        StudentQuestion question = new StudentQuestion(
                null,
                section,
                normalizeOptionalText(request.nickname()),
                request.question().trim(),
                StudentQuestionStatus.PENDING,
                now,
                now);
        StudentQuestion savedQuestion = studentQuestionRepository.save(question);

        if (storedImage != null) {
            questionAttachmentRepository.save(new QuestionAttachment(
                    null,
                    savedQuestion,
                    storedImage.publicId(),
                    storedImage.secureUrl(),
                    storedImage.format(),
                    storedImage.width(),
                    storedImage.height(),
                    storedImage.bytes(),
                    now));
        }

        return new CreateStudentQuestionResponse(
                savedQuestion.getId(),
                savedQuestion.getStatus(),
                savedQuestion.getCreatedAt());
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
