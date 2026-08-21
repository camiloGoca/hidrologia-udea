package edu.udea.hidrologia.question.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.entity.StudentQuestion;

public interface StudentQuestionRepository extends JpaRepository<StudentQuestion, Long> {

    @EntityGraph(attributePaths = { "section", "attachment" })
    Page<StudentQuestion> findByStatus(StudentQuestionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "section", "attachment" })
    @Query("""
            select q
            from StudentQuestion q
            where q.id = :id
            """)
    Optional<StudentQuestion> findByIdWithSectionAndAttachment(@Param("id") Long id);
}
