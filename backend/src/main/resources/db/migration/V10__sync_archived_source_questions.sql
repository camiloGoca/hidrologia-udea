UPDATE student_questions q
SET status = 'ARCHIVED',
    updated_at = GREATEST(q.updated_at, p.updated_at)
FROM posts p
WHERE p.source_question_id = q.id
  AND p.status = 'ARCHIVED'
  AND q.status = 'PUBLISHED';
