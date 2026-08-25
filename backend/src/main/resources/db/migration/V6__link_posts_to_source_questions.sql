ALTER TABLE posts
    ADD COLUMN source_question_id BIGINT;

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_source_question
        FOREIGN KEY (source_question_id)
        REFERENCES student_questions (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT uq_posts_source_question UNIQUE (source_question_id);

ALTER TABLE posts
    DROP CONSTRAINT chk_posts_title_not_blank,
    DROP CONSTRAINT chk_posts_content_not_blank;

ALTER TABLE posts
    ADD CONSTRAINT chk_posts_title_required_when_not_draft
        CHECK (status = 'DRAFT' OR btrim(title) <> ''),
    ADD CONSTRAINT chk_posts_content_required_when_not_draft
        CHECK (status = 'DRAFT' OR btrim(content) <> '');
