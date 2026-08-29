package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ArchivedSourceQuestionMigrationTest {

    @Test
    void v10OnlySyncsPublishedSourceQuestionsForArchivedPosts() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V10__sync_archived_source_questions.sql"));

        assertThat(migration).contains("UPDATE student_questions q");
        assertThat(migration).contains("FROM posts p");
        assertThat(migration).contains("p.source_question_id = q.id");
        assertThat(migration).contains("p.status = 'ARCHIVED'");
        assertThat(migration).contains("q.status = 'PUBLISHED'");
        assertThat(migration).contains("status = 'ARCHIVED'");
        assertThat(migration).doesNotContain("ALTER TABLE");
        assertThat(migration).doesNotContain("CREATE TABLE");
        assertThat(migration).doesNotContain("DROP");
    }
}
