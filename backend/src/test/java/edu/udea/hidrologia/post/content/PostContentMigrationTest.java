package edu.udea.hidrologia.post.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PostContentMigrationTest {

    @Test
    void v7AddsStructuredContentDocumentAndBackfillsFromPlainText() throws IOException {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V7__add_post_content_document.sql"));

        assertThat(migration).contains("ADD COLUMN content_document JSONB");
        assertThat(migration).contains("regexp_split_to_table(content");
        assertThat(migration).contains("btrim(content) = ''");
        assertThat(migration).contains("ALTER COLUMN content_document SET NOT NULL");
        assertThat(migration).contains("chk_posts_content_document_is_object");
        assertThat(migration).contains("chk_posts_content_document_root_doc");
    }
}
