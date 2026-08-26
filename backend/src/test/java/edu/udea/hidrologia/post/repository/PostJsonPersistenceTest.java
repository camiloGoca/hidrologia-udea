package edu.udea.hidrologia.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@SpringJUnitConfig(PostJsonPersistenceTest.JpaTestConfig.class)
@Transactional
class PostJsonPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T11:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Test
    void persistsAndReloadsDraftContentDocument() {
        Section section = persistSection();
        Post post = Post.createManualDraft(section, NOW);

        Post saved = postRepository.saveAndFlush(post);
        entityManager.clear();

        Post reloaded = postRepository.findAdminById(saved.getId()).orElseThrow();

        assertThat(reloaded.getContentDocument())
                .containsEntry("type", "doc")
                .containsKey("content");
        assertThat(reloaded.getContentDocument().get("content"))
                .asList()
                .singleElement()
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("type", "paragraph");
    }

    @Test
    void updatesFlushesAndReloadsContentDocument() {
        Section section = persistSection();
        Post post = postRepository.saveAndFlush(Post.createManualDraft(section, NOW));
        entityManager.clear();
        Post reloaded = postRepository.findAdminById(post.getId()).orElseThrow();
        Map<String, Object> document = Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", "Documento actualizado")))));

        reloaded.update("Titulo", "Documento actualizado", document, section, UPDATED_AT);
        postRepository.saveAndFlush(reloaded);
        entityManager.clear();

        Post updated = postRepository.findAdminById(post.getId()).orElseThrow();

        assertThat(updated.getContentDocument())
                .containsEntry("type", "doc")
                .containsEntry("content", document.get("content"));
        assertThat(updated.getContent()).isEqualTo("Documento actualizado");
        assertThat(updated.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void persistsReloadsAndUpdatesContentDocumentWithCanonicalLink() {
        Section section = persistSection();
        Post post = postRepository.saveAndFlush(Post.createManualDraft(section, NOW));
        entityManager.clear();
        Post reloaded = postRepository.findAdminById(post.getId()).orElseThrow();
        Map<String, Object> document = Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", "UdeA",
                                "marks", List.of(Map.of(
                                        "type", "link",
                                        "attrs", Map.of("href", "https://www.udea.edu.co"))))))));

        reloaded.update("Titulo", "UdeA", document, section, UPDATED_AT);
        postRepository.saveAndFlush(reloaded);
        entityManager.clear();

        Post updated = postRepository.findAdminById(post.getId()).orElseThrow();

        assertThat(updated.getContentDocument())
                .containsEntry("type", "doc")
                .containsEntry("content", document.get("content"));
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
            dataSource.setURL("jdbc:h2:mem:post_json_persistence;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
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
