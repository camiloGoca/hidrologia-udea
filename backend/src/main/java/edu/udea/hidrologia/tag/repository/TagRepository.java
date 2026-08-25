package edu.udea.hidrologia.tag.repository;

import java.util.Optional;
import java.util.Set;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.udea.hidrologia.tag.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query(value = """
            select
                t.id as id,
                t.name as name,
                t.slug as slug,
                count(pt.post_id) as usageCount
            from tags t
            left join post_tags pt on pt.tag_id = t.id
            group by t.id, t.name, t.slug
            order by lower(t.name) asc, t.slug asc
            """, nativeQuery = true)
    List<TagUsageProjection> findAllWithUsageCount();

    @Query(value = """
            select count(pt.post_id)
            from post_tags pt
            where pt.tag_id = :id
            """, nativeQuery = true)
    long countUsageById(@Param("id") Long id);

    List<Tag> findByIdIn(Set<Long> ids);
}
