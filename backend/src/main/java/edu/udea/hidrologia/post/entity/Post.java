package edu.udea.hidrologia.post.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.tag.entity.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_document", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contentDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_question_id", unique = true)
    private StudentQuestion sourceQuestion;

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Post() {
    }

    public Post(
            Long id,
            Section section,
            String title,
            String content,
            PostStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Set<Tag> tags) {
        this(id, section, title, content, documentFromPlainText(content), status, createdAt, updatedAt, publishedAt, tags, null);
    }

    public Post(
            Long id,
            Section section,
            String title,
            String content,
            PostStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Set<Tag> tags,
            StudentQuestion sourceQuestion) {
        this(id, section, title, content, documentFromPlainText(content), status, createdAt, updatedAt, publishedAt, tags, sourceQuestion);
    }

    public Post(
            Long id,
            Section section,
            String title,
            String content,
            Map<String, Object> contentDocument,
            PostStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Set<Tag> tags,
            StudentQuestion sourceQuestion) {
        validateEditorialContent(title, content, status);
        this.id = id;
        this.section = section;
        this.title = title;
        this.content = content;
        this.contentDocument = contentDocument == null ? emptyContentDocument() : deepCopyDocument(contentDocument);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        this.sourceQuestion = sourceQuestion;
    }

    public static Post createQuestionDraft(StudentQuestion question, Instant now) {
        return new Post(
                null,
                question.getSection(),
                "",
                "",
                emptyContentDocument(),
                PostStatus.DRAFT,
                now,
                now,
                null,
                new LinkedHashSet<>(),
                question);
    }

    public static Post createManualDraft(Section section, Instant now) {
        return new Post(
                null,
                section,
                "",
                "",
                emptyContentDocument(),
                PostStatus.DRAFT,
                now,
                now,
                null,
                new LinkedHashSet<>(),
                null);
    }

    public void update(String title, String content, Map<String, Object> contentDocument, Section section, Instant updatedAt) {
        validateEditorialContent(title, content, status);
        this.title = title;
        this.content = content;
        this.contentDocument = deepCopyDocument(contentDocument);
        this.section = section;
        this.updatedAt = updatedAt;
    }

    public void update(String title, String content, Section section, Instant updatedAt) {
        update(title, content, documentFromPlainText(content), section, updatedAt);
    }

    public void replaceTags(Collection<Tag> tags, Instant updatedAt) {
        this.tags.clear();
        this.tags.addAll(tags);
        this.updatedAt = updatedAt;
    }

    public void publish(Instant publishedAt) {
        if (status != PostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be published");
        }

        validateEditorialContent(title, content, PostStatus.PUBLISHED);
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.updatedAt = publishedAt;
    }

    public void archive(Instant updatedAt) {
        if (status != PostStatus.PUBLISHED) {
            throw new IllegalStateException("Only published posts can be archived");
        }

        this.status = PostStatus.ARCHIVED;
        this.updatedAt = updatedAt;
    }

    public void restore(Instant updatedAt) {
        if (status != PostStatus.ARCHIVED) {
            throw new IllegalStateException("Only archived posts can be restored");
        }

        validateEditorialContent(title, content, PostStatus.PUBLISHED);
        this.status = PostStatus.PUBLISHED;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Section getSection() {
        return section;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getContentDocument() {
        return deepCopyDocument(contentDocument);
    }

    public PostStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public StudentQuestion getSourceQuestion() {
        return sourceQuestion;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    private void validateEditorialContent(String title, String content, PostStatus status) {
        if (status != PostStatus.DRAFT && isBlank(title)) {
            throw new IllegalArgumentException("Post title is required outside draft status");
        }

        if (status != PostStatus.DRAFT && isBlank(content)) {
            throw new IllegalArgumentException("Post content is required outside draft status");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Map<String, Object> emptyContentDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("type", "doc");
        document.put("content", List.of(Map.of("type", "paragraph")));

        return document;
    }

    private static Map<String, Object> documentFromPlainText(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return emptyContentDocument();
        }

        Map<String, Object> document = new LinkedHashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();
        for (String line : plainText.split("\\R", -1)) {
            Map<String, Object> paragraph = new LinkedHashMap<>();
            paragraph.put("type", "paragraph");
            if (!line.isEmpty()) {
                paragraph.put("content", List.of(Map.of(
                        "type", "text",
                        "text", line)));
            }
            content.add(paragraph);
        }
        document.put("type", "doc");
        document.put("content", content);

        return document;
    }

    private static Map<String, Object> deepCopyDocument(Map<String, Object> document) {
        Map<String, Object> copy = new LinkedHashMap<>();
        document.forEach((key, value) -> copy.put(key, deepCopyValue(value)));

        return copy;
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, entry) -> copy.put(String.valueOf(key), deepCopyValue(entry)));

            return copy;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(Post::deepCopyValue)
                    .toList();
        }

        return value;
    }
}
