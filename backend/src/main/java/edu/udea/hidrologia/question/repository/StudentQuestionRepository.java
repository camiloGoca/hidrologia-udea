package edu.udea.hidrologia.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.question.entity.StudentQuestion;

public interface StudentQuestionRepository extends JpaRepository<StudentQuestion, Long> {
}
