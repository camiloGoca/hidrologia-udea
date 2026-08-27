package edu.udea.hidrologia.post.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.post.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderById(Long postId);

    List<PostImage> findByPostIdAndIdInOrderById(Long postId, Set<Long> ids);

    @EntityGraph(attributePaths = "post")
    Optional<PostImage> findByIdAndPostId(Long id, Long postId);

    long countByPostIdAndIdIn(Long postId, Set<Long> ids);
}
