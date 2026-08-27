package edu.udea.hidrologia.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@SpringJUnitConfig(PostImagePersistenceTest.JpaTestConfig.class)
@Transactional
class PostImagePersistenceTest {

    private static final Instant NOW = Instant.parse("2026-01-03T00:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Test
    void persistsAndReloadsPostImageMetadata() {
        Post post = postRepository.saveAndFlush(Post.createManualDraft(persistSection(), NOW));
        PostImage image = postImageRepository.saveAndFlush(new PostImage(
                null,
                post,
                "hidrologia-udea/posts/1/post-1-image-1",
                "https://res.cloudinary.com/demo/image/upload/post-1-image-1.png",
                "png",
                800,
                600,
                1000L,
                "Diagrama de cuenca",
                NOW));

        entityManager.clear();

        PostImage reloaded = postImageRepository.findByIdAndPostId(image.getId(), post.getId()).orElseThrow();

        assertThat(reloaded.getPost().getId()).isEqualTo(post.getId());
        assertThat(reloaded.getPublicId()).isEqualTo("hidrologia-udea/posts/1/post-1-image-1");
        assertThat(reloaded.getSecureUrl()).startsWith("https://");
        assertThat(reloaded.getFormat()).isEqualTo("png");
        assertThat(reloaded.getAltText()).isEqualTo("Diagrama de cuenca");
    }

    @Test
    void enforcesPostImageConstraints() {
        Post post = postRepository.saveAndFlush(Post.createManualDraft(persistSection(), NOW));

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                    INSERT INTO post_images
                        (post_id, public_id, secure_url, format, width, height, bytes, alt_text, created_at)
                    VALUES
                        (:postId, 'hidrologia-udea/posts/1/bad', 'https://example.com/bad.png',
                         'png', 0, 600, 1000, 'Imagen', CURRENT_TIMESTAMP)
                    """)
                    .setParameter("postId", post.getId())
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    void restrictsPostDeleteWhenPostImagesExist() {
        Post post = postRepository.saveAndFlush(Post.createManualDraft(persistSection(), NOW));
        postImageRepository.saveAndFlush(new PostImage(
                null,
                post,
                "hidrologia-udea/posts/1/post-1-image-1",
                "https://res.cloudinary.com/demo/image/upload/post-1-image-1.png",
                "jpg",
                800,
                600,
                1000L,
                "Diagrama de cuenca",
                NOW));

        assertThatThrownBy(() -> {
            postRepository.delete(post);
            postRepository.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    private Section persistSection() {
        Section section = new Section(
                null,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometría de cuencas",
                1,
                true,
                NOW);
        entityManager.persist(section);
        entityManager.flush();

        return section;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = PostRepository.class)
    static class JpaTestConfig {

        @Bean
        DataSource dataSource() throws SQLException {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:post_image_persistence;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                    + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;"
                    + "INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON");
            dataSource.setUser("sa");
            dataSource.setPassword("");
            initializeSchema(dataSource);

            return dataSource;
        }

        private static void initializeSchema(DataSource dataSource) throws SQLException {
            String schema = """
                    CREATE TABLE sections (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        name VARCHAR(120) NOT NULL,
                        slug VARCHAR(140) NOT NULL,
                        description VARCHAR,
                        display_order INTEGER NOT NULL,
                        active BOOLEAN NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL
                    );

                    CREATE TABLE student_questions (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        section_id BIGINT NOT NULL REFERENCES sections(id),
                        nickname VARCHAR(80),
                        question VARCHAR NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                    );

                    CREATE TABLE question_attachments (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        question_id BIGINT NOT NULL UNIQUE REFERENCES student_questions(id),
                        public_id VARCHAR(255) NOT NULL UNIQUE,
                        secure_url VARCHAR NOT NULL,
                        format VARCHAR(20) NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        bytes BIGINT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL
                    );

                    CREATE TABLE posts (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        section_id BIGINT NOT NULL REFERENCES sections(id),
                        title VARCHAR(180) NOT NULL,
                        content VARCHAR NOT NULL,
                        content_document JSONB NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        published_at TIMESTAMP WITH TIME ZONE,
                        source_question_id BIGINT UNIQUE REFERENCES student_questions(id)
                    );

                    CREATE TABLE tags (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        name VARCHAR(80) NOT NULL,
                        slug VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL
                    );

                    CREATE TABLE post_tags (
                        post_id BIGINT NOT NULL REFERENCES posts(id),
                        tag_id BIGINT NOT NULL REFERENCES tags(id),
                        PRIMARY KEY (post_id, tag_id)
                    );

                    CREATE TABLE post_images (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        post_id BIGINT NOT NULL,
                        public_id VARCHAR(255) NOT NULL UNIQUE,
                        secure_url VARCHAR NOT NULL,
                        format VARCHAR(20) NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        bytes BIGINT NOT NULL,
                        alt_text VARCHAR(180) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE RESTRICT,
                        CONSTRAINT chk_post_images_width_positive CHECK (width > 0),
                        CONSTRAINT chk_post_images_height_positive CHECK (height > 0),
                        CONSTRAINT chk_post_images_bytes_positive CHECK (bytes > 0),
                        CONSTRAINT chk_post_images_alt_text_not_blank CHECK (btrim(alt_text) <> '')
                    );
                    """;

            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setPackagesToScan("edu.udea.hidrologia");
            factory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", "none",
                    "hibernate.show_sql", "false"));

            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
