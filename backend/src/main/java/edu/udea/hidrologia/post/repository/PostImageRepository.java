package edu.udea.hidrologia.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.post.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderById(Long postId);

    @EntityGraph(attributePaths = "post")
    Optional<PostImage> findByIdAndPostId(Long id, Long postId);

    boolean existsByPostId(Long postId);
}
