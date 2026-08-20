package edu.udea.hidrologia.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.question.entity.QuestionAttachment;

public interface QuestionAttachmentRepository extends JpaRepository<QuestionAttachment, Long> {
}
