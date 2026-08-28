package edu.udea.hidrologia.analytics.repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.section.entity.SectionType;

@Repository
public class AnalyticsRepository {

    private static final String BOGOTA_TIMEZONE = "America/Bogota";

    private final JdbcClient jdbcClient;

    public AnalyticsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insertSiteVisit(UUID sessionId, Instant visitedAt) {
        updateIdempotently("""
                INSERT INTO site_visits (session_id, visited_at)
                VALUES (:sessionId, :visitedAt)
                ON CONFLICT (session_id) DO NOTHING
                """, sessionId, "visitedAt", visitedAt, null);
    }

    public void insertSectionView(UUID sessionId, long sectionId, Instant viewedAt) {
        updateIdempotently("""
                INSERT INTO section_views (session_id, section_id, viewed_at)
                VALUES (:sessionId, :resourceId, :viewedAt)
                ON CONFLICT (session_id, section_id) DO NOTHING
                """, sessionId, "viewedAt", viewedAt, sectionId);
    }

    public void insertPostView(UUID sessionId, long postId, Instant viewedAt) {
        updateIdempotently("""
                INSERT INTO post_views (session_id, post_id, viewed_at)
                VALUES (:sessionId, :resourceId, :viewedAt)
                ON CONFLICT (session_id, post_id) DO NOTHING
                """, sessionId, "viewedAt", viewedAt, postId);
    }

    public long countSiteVisits() {
        return jdbcClient.sql("SELECT COUNT(*) FROM site_visits")
                .query(Long.class)
                .single();
    }

    public long countSiteVisitsBetween(Instant startInclusive, Instant endExclusive) {
        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM site_visits
                WHERE visited_at >= :startInclusive
                  AND visited_at < :endExclusive
                """)
                .param("startInclusive", Timestamp.from(startInclusive))
                .param("endExclusive", Timestamp.from(endExclusive))
                .query(Long.class)
                .single();
    }

    public List<SectionViewsRow> findSectionViewsRanking() {
        return jdbcClient.sql("""
                SELECT s.id,
                       s.type,
                       s.name,
                       s.slug,
                       COUNT(sv.id) AS views
                FROM sections s
                LEFT JOIN section_views sv ON sv.section_id = s.id
                WHERE s.active = TRUE
                GROUP BY s.id, s.type, s.name, s.slug, s.display_order
                ORDER BY views DESC, s.display_order ASC, s.id ASC
                """)
                .query((rs, rowNum) -> new SectionViewsRow(
                        rs.getLong("id"),
                        SectionType.valueOf(rs.getString("type")),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getLong("views")))
                .list();
    }

    public List<PostViewsRow> findMostViewedPublishedPosts(int limit) {
        return jdbcClient.sql("""
                SELECT p.id,
                       p.title,
                       s.id AS section_id,
                       s.type AS section_type,
                       s.name AS section_name,
                       s.slug AS section_slug,
                       s.description AS section_description,
                       COUNT(pv.id) AS views
                FROM posts p
                JOIN sections s ON s.id = p.section_id
                JOIN post_views pv ON pv.post_id = p.id
                WHERE p.status = :status
                GROUP BY p.id, p.title, s.id, s.type, s.name, s.slug, s.description
                HAVING COUNT(pv.id) > 0
                ORDER BY views DESC, p.published_at DESC, p.id DESC
                LIMIT :limit
                """)
                .param("status", PostStatus.PUBLISHED.name())
                .param("limit", limit)
                .query((rs, rowNum) -> new PostViewsRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getLong("section_id"),
                        SectionType.valueOf(rs.getString("section_type")),
                        rs.getString("section_name"),
                        rs.getString("section_slug"),
                        rs.getString("section_description"),
                        rs.getLong("views")))
                .list();
    }

    public List<DailyVisitsRow> findDailyVisitsBetween(LocalDate startDate, LocalDate endDateInclusive) {
        return jdbcClient.sql("""
                SELECT (visited_at AT TIME ZONE :timezone)::date AS visit_date,
                       COUNT(*) AS visits
                FROM site_visits
                WHERE visited_at >= :startInstant
                  AND visited_at < :endInstant
                GROUP BY visit_date
                ORDER BY visit_date ASC
                """)
                .param("timezone", BOGOTA_TIMEZONE)
                .param("startInstant", Timestamp.from(startDate.atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant()))
                .param("endInstant", Timestamp.from(endDateInclusive.plusDays(1).atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant()))
                .query((rs, rowNum) -> new DailyVisitsRow(
                        toLocalDate(rs.getObject("visit_date")),
                        rs.getLong("visits")))
                .list();
    }

    public QuestionCountsRow findQuestionCounts() {
        return jdbcClient.sql("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = :pendingStatus) AS pending,
                       COUNT(*) FILTER (WHERE status = :publishedStatus) AS published
                FROM student_questions
                """)
                .param("pendingStatus", StudentQuestionStatus.PENDING.name())
                .param("publishedStatus", StudentQuestionStatus.PUBLISHED.name())
                .query((rs, rowNum) -> new QuestionCountsRow(
                        rs.getLong("total"),
                        rs.getLong("pending"),
                        rs.getLong("published")))
                .single();
    }

    public Optional<Long> findActiveSectionIdBySlug(String slug) {
        return jdbcClient.sql("""
                SELECT id
                FROM sections
                WHERE slug = :slug
                  AND active = TRUE
                """)
                .param("slug", slug)
                .query(Long.class)
                .optional();
    }

    public Optional<Long> findPublishedPostId(long postId) {
        return jdbcClient.sql("""
                SELECT id
                FROM posts
                WHERE id = :postId
                  AND status = :status
                """)
                .param("postId", postId)
                .param("status", PostStatus.PUBLISHED.name())
                .query(Long.class)
                .optional();
    }

    private void updateIdempotently(String sql, UUID sessionId, String instantParam, Instant instant, Long resourceId) {
        try {
            JdbcClient.StatementSpec spec = jdbcClient.sql(sql)
                    .param("sessionId", sessionId)
                    .param(instantParam, Timestamp.from(instant));
            if (resourceId != null) {
                spec = spec.param("resourceId", resourceId);
            }
            spec.update();
        } catch (DataIntegrityViolationException ignored) {
            // A concurrent request can lose the ON CONFLICT race on some databases/tests.
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        return LocalDate.parse(String.valueOf(value));
    }

    public record SectionViewsRow(Long id, SectionType type, String name, String slug, long views) {
    }

    public record PostViewsRow(
            Long id,
            String title,
            Long sectionId,
            SectionType sectionType,
            String sectionName,
            String sectionSlug,
            String sectionDescription,
            long views) {
    }

    public record DailyVisitsRow(LocalDate date, long visits) {
    }

    public record QuestionCountsRow(long total, long pending, long published) {
    }
}
