package edu.udea.hidrologia.tag.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.tag.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);
}
