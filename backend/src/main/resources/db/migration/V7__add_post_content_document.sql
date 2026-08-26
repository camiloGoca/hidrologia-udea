ALTER TABLE posts
    ADD COLUMN content_document JSONB;

UPDATE posts
SET content_document = jsonb_build_object(
        'type', 'doc',
        'content', CASE
            WHEN btrim(content) = '' THEN jsonb_build_array(jsonb_build_object('type', 'paragraph'))
            ELSE (
                SELECT jsonb_agg(
                        CASE
                            WHEN line = '' THEN jsonb_build_object('type', 'paragraph')
                            ELSE jsonb_build_object(
                                    'type', 'paragraph',
                                    'content', jsonb_build_array(jsonb_build_object('type', 'text', 'text', line)))
                        END
                        ORDER BY line_number)
                FROM regexp_split_to_table(content, E'\\r?\\n') WITH ORDINALITY AS lines(line, line_number)
            )
        END
);

ALTER TABLE posts
    ALTER COLUMN content_document SET NOT NULL,
    ADD CONSTRAINT chk_posts_content_document_is_object
        CHECK (jsonb_typeof(content_document) = 'object'),
    ADD CONSTRAINT chk_posts_content_document_root_doc
        CHECK (
            content_document ? 'type'
            AND content_document ->> 'type' = 'doc'
        );
