package edu.udea.hidrologia.question.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class StudentQuestionService {

    private final StudentQuestionRepository studentQuestionRepository;
    private final SectionRepository sectionRepository;
    private final Clock clock;

    public StudentQuestionService(
            StudentQuestionRepository studentQuestionRepository,
            SectionRepository sectionRepository,
            Clock clock) {
        this.studentQuestionRepository = studentQuestionRepository;
        this.sectionRepository = sectionRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateStudentQuestionResponse createQuestion(CreateStudentQuestionRequest request) {
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
