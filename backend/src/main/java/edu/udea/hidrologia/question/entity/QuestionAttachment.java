package edu.udea.hidrologia.question.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_attachments")
public class QuestionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private StudentQuestion question;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "secure_url", nullable = false)
    private String secureUrl;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Long bytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuestionAttachment() {
    }

    public QuestionAttachment(
            Long id,
            StudentQuestion question,
            String publicId,
            String secureUrl,
            String format,
            Integer width,
            Integer height,
            Long bytes,
            Instant createdAt) {
        this.id = id;
        this.question = question;
        this.publicId = publicId;
        this.secureUrl = secureUrl;
        this.format = format;
        this.width = width;
        this.height = height;
        this.bytes = bytes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public StudentQuestion getQuestion() {
        return question;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public String getFormat() {
        return format;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Long getBytes() {
        return bytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
