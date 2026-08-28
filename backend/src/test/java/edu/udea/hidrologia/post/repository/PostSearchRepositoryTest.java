package edu.udea.hidrologia.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.tag.entity.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@SpringJUnitConfig(PostSearchRepositoryTest.JpaTestConfig.class)
@Transactional
class PostSearchRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant OLDER_PUBLISHED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant NEWER_PUBLISHED_AT = Instant.parse("2026-01-03T00:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Test
    void searchesOnlyPublishedPostsByTitleContentAndTagsWithoutDuplicates() {
        Section section = persistSection();
        Tag balance = persistTag("Balance hidrico", "balance-hidrico");
        Tag cuencas = persistTag("Cuencas", "cuencas");

        Post contentMatch = persistPost(
                section,
                "Analisis de caudal",
                "Contenido con balance para encontrar",
                PostStatus.PUBLISHED,
                NEWER_PUBLISHED_AT,
                Set.of(cuencas));
        Post titleMatch = persistPost(
                section,
                "Balance en cuencas",
                "Contenido general",
                PostStatus.PUBLISHED,
                OLDER_PUBLISHED_AT,
                Set.of(cuencas));
        Post tagMatch = persistPost(
                section,
                "Estadistica",
                "Contenido general",
                PostStatus.PUBLISHED,
                OLDER_PUBLISHED_AT,
                Set.of(balance, cuencas));
        persistPost(section, "Balance borrador", "Contenido", PostStatus.DRAFT, null, Set.of(balance));
        persistPost(section, "Balance archivado", "Contenido", PostStatus.ARCHIVED, OLDER_PUBLISHED_AT, Set.of(balance));
        entityManager.flush();
        entityManager.clear();

        List<Post> results = postRepository.searchPublishedPosts("%balance%", PostStatus.PUBLISHED);

        assertThat(results)
                .extracting(Post::getId)
                .containsExactly(titleMatch.getId(), contentMatch.getId(), tagMatch.getId());
    }

    @Test
    void searchesCaseInsensitiveAndPartialTagSlug() {
        Section section = persistSection();
        Tag tag = persistTag("Precipitacion", "precipitacion-media");
        Post post = persistPost(
                section,
                "Titulo",
                "Contenido",
                PostStatus.PUBLISHED,
                NEWER_PUBLISHED_AT,
                Set.of(tag));
        entityManager.flush();
        entityManager.clear();

        List<Post> results = postRepository.searchPublishedPosts("%PRECIP%", PostStatus.PUBLISHED);

        assertThat(results).extracting(Post::getId).containsExactly(post.getId());
    }

    private Section persistSection() {
        Section section = new Section(
                null,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas",
                1,
                true,
                CREATED_AT);
        entityManager.persist(section);

        return section;
    }

    private Tag persistTag(String name, String slug) {
        Tag tag = new Tag(null, name, slug, CREATED_AT);
        entityManager.persist(tag);

        return tag;
    }

    private Post persistPost(
            Section section,
            String title,
            String content,
            PostStatus status,
            Instant publishedAt,
            Set<Tag> tags) {
        Post post = new Post(
                null,
                section,
                title,
                content,
                status,
                CREATED_AT,
                CREATED_AT,
                publishedAt,
                new LinkedHashSet<>(tags));
        entityManager.persist(post);

        return post;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = PostRepository.class)
    static class JpaTestConfig {

        @Bean
        DataSource dataSource() throws SQLException {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:post_search_repository;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
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
