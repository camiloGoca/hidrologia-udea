package edu.udea.hidrologia.post.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.section.entity.Section;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = { "section", "tags" })
    @Query("""
            select distinct p
            from Post p
            where p.section = :section
              and p.status = :status
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> findBySectionAndStatusOrderByPublishedAtDescIdDesc(Section section, PostStatus status);

    @EntityGraph(attributePaths = { "section", "tags" })
    Optional<Post> findByIdAndStatus(Long id, PostStatus status);

    @EntityGraph(attributePaths = { "section", "sourceQuestion", "sourceQuestion.section", "sourceQuestion.attachment" })
    @Query("""
            select p
            from Post p
            where p.id = :id
            """)
    Optional<Post> findAdminById(@Param("id") Long id);

    boolean existsBySourceQuestionId(Long sourceQuestionId);

    @EntityGraph(attributePaths = { "sourceQuestion" })
    Optional<Post> findBySourceQuestionId(Long sourceQuestionId);

    @EntityGraph(attributePaths = { "sourceQuestion" })
    List<Post> findBySourceQuestionIdIn(Set<Long> sourceQuestionIds);

    @EntityGraph(attributePaths = { "section", "tags" })
    @Query("""
            select distinct p
            from Post p
            join p.tags tag
            where tag.slug = :slug
              and p.status = :status
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> findByTagSlugAndStatusOrderByPublishedAtDescIdDesc(
            @Param("slug") String slug,
            @Param("status") PostStatus status);
}
