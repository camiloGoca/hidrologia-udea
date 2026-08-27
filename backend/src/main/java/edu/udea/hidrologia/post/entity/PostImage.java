package edu.udea.hidrologia.post.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_images")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

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

    @Column(name = "alt_text", nullable = false, length = 180)
    private String altText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PostImage() {
    }

    public PostImage(
            Long id,
            Post post,
            String publicId,
            String secureUrl,
            String format,
            Integer width,
            Integer height,
            Long bytes,
            String altText,
            Instant createdAt) {
        this.id = id;
        this.post = post;
        this.publicId = publicId;
        this.secureUrl = secureUrl;
        this.format = format;
        this.width = width;
        this.height = height;
        this.bytes = bytes;
        this.altText = altText;
        this.createdAt = createdAt;
    }

    public void updateAltText(String altText) {
        this.altText = altText;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
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

    public String getAltText() {
        return altText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
