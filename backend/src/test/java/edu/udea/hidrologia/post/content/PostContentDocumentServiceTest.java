package edu.udea.hidrologia.post.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class PostContentDocumentServiceTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final PostContentDocumentService service = new PostContentDocumentService(jsonMapper);

    @Test
    void acceptsEmptyDraftDocument() {
        Map<String, Object> document = service.emptyDocument();

        assertThat(service.validate(document)).containsEntry("type", "doc");
        assertThat(service.extractPlainText(document)).isEmpty();
    }

    @Test
    void extractsPlainTextFromAcademicNodes() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": { "level": 2 },
                      "content": [{ "type": "text", "text": "Subtítulo" }]
                    },
                    {
                      "type": "paragraph",
                      "content": [
                        { "type": "text", "text": "Línea 1" },
                        { "type": "hardBreak" },
                        { "type": "text", "text": "Línea 2" }
                      ]
                    },
                    {
                      "type": "bulletList",
                      "content": [
                        {
                          "type": "listItem",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [{ "type": "text", "text": "Elemento" }]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "type": "blockquote",
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [{ "type": "text", "text": "Cita" }]
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThat(service.extractPlainText(document))
                .contains("Subtítulo")
                .contains("Línea 1\nLínea 2")
                .contains("Elemento")
                .contains("Cita");
    }

    @Test
    void acceptsAllowedMarksAndSafeLinks() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "Texto",
                          "marks": [
                            { "type": "bold" },
                            { "type": "italic" },
                            { "type": "underline" },
                            { "type": "link", "attrs": { "href": "https://udea.edu.co" } }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThat(service.extractPlainText(document)).isEqualTo("Texto");
    }

    @Test
    void acceptsHttpLinksWhenUsingApprovedProtocol() throws Exception {
        Map<String, Object> document = documentWithLink("http://www.udea.edu.co");

        Map<String, Object> validated = service.validate(document);

        assertThat(linkAttrs(validated)).containsOnly(Map.entry("href", "http://www.udea.edu.co"));
    }

    @Test
    void normalizesTiptapLinkDefaultAttributesToCanonicalHrefOnly() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "UdeA",
                          "marks": [
                            {
                              "type": "link",
                              "attrs": {
                                "href": "https://www.udea.edu.co",
                                "target": "_blank",
                                "rel": "noopener noreferrer",
                                "class": null,
                                "title": null
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);

        Map<String, Object> validated = service.validate(document);

        assertThat(linkAttrs(validated)).containsOnly(Map.entry("href", "https://www.udea.edu.co"));
    }

    @Test
    void acceptsH2AndH3HeadingsButRejectsH1() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": { "level": 2 },
                      "content": [{ "type": "text", "text": "Subtitulo principal" }]
                    },
                    {
                      "type": "heading",
                      "attrs": { "level": 3 },
                      "content": [{ "type": "text", "text": "Subtitulo secundario" }]
                    }
                  ]
                }
                """);
        Map<String, Object> h1Document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": { "level": 1 },
                      "content": [{ "type": "text", "text": "Titulo interno" }]
                    }
                  ]
                }
                """);

        assertThat(service.extractPlainText(document))
                .contains("Subtitulo principal")
                .contains("Subtitulo secundario");
        assertThatThrownBy(() -> service.validate(h1Document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void acceptsControlledTextSizesAndRejectsArbitrarySizes() throws Exception {
        Map<String, Object> document = documentWithMark("""
                { "type": "textSize", "attrs": { "size": "large" } }
                """);
        Map<String, Object> invalidDocument = documentWithMark("""
                { "type": "textSize", "attrs": { "size": "27px" } }
                """);

        Map<String, Object> validated = service.validate(document);

        assertThat(markAttrs(validated)).containsOnly(Map.entry("size", "large"));
        assertThatThrownBy(() -> service.validate(invalidDocument))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void acceptsControlledTextColorsAndRejectsArbitraryColors() throws Exception {
        Map<String, Object> document = documentWithMark("""
                { "type": "textColor", "attrs": { "color": "institutional" } }
                """);
        Map<String, Object> invalidDocument = documentWithMark("""
                { "type": "textColor", "attrs": { "color": "#00ff00" } }
                """);

        Map<String, Object> validated = service.validate(document);

        assertThat(markAttrs(validated)).containsOnly(Map.entry("color", "institutional"));
        assertThatThrownBy(() -> service.validate(invalidDocument))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsControlledAlignmentAndRejectsUnknownAlignment() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "attrs": { "textAlign": "justify" },
                      "content": [{ "type": "text", "text": "Texto justificado" }]
                    }
                  ]
                }
                """);
        Map<String, Object> invalidDocument = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "attrs": { "textAlign": "diagonal" },
                      "content": [{ "type": "text", "text": "Texto" }]
                    }
                  ]
                }
                """);

        Map<String, Object> validated = service.validate(document);
        List<Map<String, Object>> content = (List<Map<String, Object>>) validated.get("content");

        assertThat((Map<String, Object>) content.get(0).get("attrs"))
                .containsOnly(Map.entry("textAlign", "justify"));
        assertThatThrownBy(() -> service.validate(invalidDocument))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void acceptsControlledHighlightAndRejectsArbitraryHighlight() throws Exception {
        Map<String, Object> document = documentWithMark("""
                { "type": "highlight", "attrs": { "kind": "important" } }
                """);
        Map<String, Object> invalidDocument = documentWithMark("""
                { "type": "highlight", "attrs": { "kind": "rainbow" } }
                """);

        Map<String, Object> validated = service.validate(document);

        assertThat(markAttrs(validated)).containsOnly(Map.entry("kind", "important"));
        assertThatThrownBy(() -> service.validate(invalidDocument))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAcademicBlocksAndRejectsUnknownKinds() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "academicBlock",
                      "attrs": { "kind": "example" },
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [{ "type": "text", "text": "Ejemplo resuelto" }]
                        }
                      ]
                    }
                  ]
                }
                """);
        Map<String, Object> invalidDocument = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "academicBlock",
                      "attrs": { "kind": "warning" },
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [{ "type": "text", "text": "Texto" }]
                        }
                      ]
                    }
                  ]
                }
                """);

        Map<String, Object> validated = service.validate(document);
        List<Map<String, Object>> content = (List<Map<String, Object>>) validated.get("content");

        assertThat((Map<String, Object>) content.get(0).get("attrs"))
                .containsOnly(Map.entry("kind", "example"));
        assertThat(service.extractPlainText(document)).isEqualTo("Ejemplo resuelto");
        assertThatThrownBy(() -> service.validate(invalidDocument))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void extractorIgnoresControlledStyles() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "attrs": { "textAlign": "center" },
                      "content": [
                        {
                          "type": "text",
                          "text": "Texto con estilo",
                          "marks": [
                            { "type": "textSize", "attrs": { "size": "small" } },
                            { "type": "textColor", "attrs": { "color": "blue" } },
                            { "type": "highlight", "attrs": { "kind": "note" } }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThat(service.extractPlainText(document)).isEqualTo("Texto con estilo");
    }

    @Test
    void rejectsUnknownNodes() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [{ "type": "iframe" }]
                }
                """);

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAndCanonicalizesImageNodes() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": {
                        "postImageId": 15,
                        "caption": "  Perfil longitudinal  "
                      }
                    }
                  ]
                }
                """);

        Map<String, Object> validated = service.validate(document);
        List<Map<String, Object>> content = (List<Map<String, Object>>) validated.get("content");
        Map<String, Object> attrs = (Map<String, Object>) content.get(0).get("attrs");

        assertThat(attrs).containsOnly(
                Map.entry("postImageId", 15L),
                Map.entry("caption", "Perfil longitudinal"),
                Map.entry("displaySize", "medium"));
        assertThat(service.referencesPostImageId(document, 15L)).isTrue();
        assertThat(service.referencesPostImageId(document, 16L)).isFalse();
        assertThat(service.referencedPostImageIds(document)).containsExactly(15L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsControlledImageDisplaySizes() throws Exception {
        for (String displaySize : List.of("small", "medium", "large")) {
            Map<String, Object> document = imageDocument("""
                    "postImageId": 15,
                    "displaySize": "%s"
                    """.formatted(displaySize));

            Map<String, Object> validated = service.validate(document);
            List<Map<String, Object>> content = (List<Map<String, Object>>) validated.get("content");
            Map<String, Object> attrs = (Map<String, Object>) content.get(0).get("attrs");

            assertThat(attrs).containsEntry("displaySize", displaySize);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void defaultsMissingImageDisplaySizeToMedium() throws Exception {
        Map<String, Object> document = imageDocument("""
                "postImageId": 15
                """);

        Map<String, Object> validated = service.validate(document);
        List<Map<String, Object>> content = (List<Map<String, Object>>) validated.get("content");
        Map<String, Object> attrs = (Map<String, Object>) content.get(0).get("attrs");

        assertThat(attrs).containsOnly(
                Map.entry("postImageId", 15L),
                Map.entry("displaySize", "medium"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void omitsBlankImageCaptionDuringCanonicalization() throws Exception {
        Map<String, Object> document = imageDocument("""
                "postImageId": 15,
                "caption": "   "
                """);

        Map<String, Object> validated = service.validate(document);
        List<Map<String, Object>> content = (List<Map<String, Object>>) validated.get("content");
        Map<String, Object> attrs = (Map<String, Object>) content.get(0).get("attrs");

        assertThat(attrs).containsOnly(
                Map.entry("postImageId", 15L),
                Map.entry("displaySize", "medium"));
    }

    @Test
    void extractsImageCaptionAsPlainText() throws Exception {
        Map<String, Object> document = imageDocument("""
                "postImageId": 15,
                "caption": "Curva observada"
                """);

        assertThat(service.extractPlainText(document)).isEqualTo("Curva observada");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAndCanonicalizesVideoNodes() throws Exception {
        Map<String, Object> youtube = videoDocument("""
                "provider": "youtube",
                "sourceUrl": "  https://www.youtube.com/watch?v=abc_DEF1234  ",
                "videoId": "abc_DEF1234"
                """);
        Map<String, Object> tiktok = videoDocument("""
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/@udea/video/1234567890123456789",
                "videoId": "1234567890123456789"
                """);
        Map<String, Object> direct = videoDocument("""
                "provider": "direct",
                "sourceUrl": "https://cdn.example.edu/videos/caudal.mp4",
                "videoId": null
                """);

        Map<String, Object> validatedYoutube = service.validate(youtube);
        Map<String, Object> validatedTiktok = service.validate(tiktok);
        Map<String, Object> validatedDirect = service.validate(direct);

        assertThat(videoAttrs(validatedYoutube)).containsOnly(
                Map.entry("provider", "youtube"),
                Map.entry("sourceUrl", "https://www.youtube.com/watch?v=abc_DEF1234"),
                Map.entry("videoId", "abc_DEF1234"));
        assertThat(videoAttrs(validatedTiktok)).containsOnly(
                Map.entry("provider", "tiktok"),
                Map.entry("sourceUrl", "https://www.tiktok.com/@udea/video/1234567890123456789"),
                Map.entry("videoId", "1234567890123456789"));
        assertThat(videoAttrs(validatedDirect))
                .containsEntry("provider", "direct")
                .containsEntry("sourceUrl", "https://cdn.example.edu/videos/caudal.mp4")
                .containsEntry("videoId", null)
                .hasSize(3);
        assertThat(service.extractPlainText(youtube)).isEmpty();
    }

    @Test
    void acceptsCommonYoutubeUrlShapes() throws Exception {
        for (String attrs : List.of(
                """
                "provider": "youtube",
                "sourceUrl": "https://youtu.be/abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/shorts/abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/embed/abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live/abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live/abc_DEF1234?si=abc123",
                "videoId": "abc_DEF1234"
                """)) {
            assertThat(service.validate(videoDocument(attrs))).containsEntry("type", "doc");
        }
    }

    @Test
    void acceptsDirectWebmVideo() throws Exception {
        Map<String, Object> document = videoDocument("""
                "provider": "direct",
                "sourceUrl": "https://cdn.example.edu/videos/caudal.webm"
                """);

        assertThat(videoAttrs(service.validate(document))).containsEntry("videoId", null);
    }

    @Test
    void rejectsInvalidVideoNodes() throws Exception {
        for (String attrs : List.of(
                """
                "provider": "vimeo",
                "sourceUrl": "https://vimeo.com/123456",
                "videoId": "123456"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://youtube.com.evil.example/watch?v=abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://youtube.com.evil.example/live/abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "http://www.youtube.com/watch?v=abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/watch?v=abc_DEF1234",
                "videoId": "different1"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live/",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live//abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live/abc_DEF1234/",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live/abc_DEF1234/extra",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/live/abc_DEF1234",
                "videoId": "different1"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/shorts//abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/embed//abc_DEF1234",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/@udea/video/1234567890",
                "videoId": "not-numeric"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/cualquier-cosa/video/123456789",
                "videoId": "123456789"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/video/123456789",
                "videoId": "123456789"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/@usuario/otro/123456789",
                "videoId": "123456789"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/@usuario//video/123456789",
                "videoId": "123456789"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/@usuario/video//123456789",
                "videoId": "123456789"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/@usuario/video/123456789/extra",
                "videoId": "123456789"
                """,
                """
                "provider": "tiktok",
                "sourceUrl": "https://www.tiktok.com/player/v1/123456789/extra",
                "videoId": "123456789"
                """,
                """
                "provider": "direct",
                "sourceUrl": "https://example.edu/video-page"
                """,
                """
                "provider": "direct",
                "sourceUrl": "https://example.edu/video.mp4",
                "videoId": "abc_DEF1234"
                """,
                """
                "provider": "direct",
                "sourceUrl": "blob:https://example.edu/video.mp4"
                """,
                """
                "provider": "youtube",
                "sourceUrl": "https://www.youtube.com/watch?v=abc_DEF1234",
                "videoId": "abc_DEF1234",
                "iframe": "<iframe></iframe>"
                """)) {
            Map<String, Object> document = videoDocument(attrs);

            assertThatThrownBy(() -> service.validate(document))
                    .isInstanceOf(InvalidPostContentDocumentException.class);
        }
    }

    @Test
    void rejectsContentInsideVideoNode() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "video",
                      "attrs": {
                        "provider": "youtube",
                        "sourceUrl": "https://www.youtube.com/watch?v=abc_DEF1234",
                        "videoId": "abc_DEF1234"
                      },
                      "content": [{ "type": "text", "text": "No permitido" }]
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void rejectsImageWithoutPositiveIntegralPostImageId() throws Exception {
        for (String attrs : List.of(
                """
                "caption": "Sin id"
                """,
                """
                "postImageId": 0
                """,
                """
                "postImageId": -1
                """,
                """
                "postImageId": "15"
                """,
                """
                "postImageId": 15.5
                """)) {
            Map<String, Object> document = imageDocument(attrs);

            assertThatThrownBy(() -> service.validate(document))
                    .isInstanceOf(InvalidPostContentDocumentException.class);
        }
    }

    @Test
    void rejectsInvalidImageAttributes() throws Exception {
        for (String attrs : List.of(
                """
                "postImageId": 15,
                "caption": "%s"
                """.formatted("a".repeat(241)),
                """
                "postImageId": 15,
                "displaySize": null
                """,
                """
                "postImageId": 15,
                "displaySize": "half"
                """,
                """
                "postImageId": 15,
                "displaySize": "360px"
                """,
                """
                "postImageId": 15,
                "displaySize": "50%"
                """,
                """
                "postImageId": 15,
                "width": 360
                """,
                """
                "postImageId": 15,
                "src": "https://example.com/image.png"
                """,
                """
                "postImageId": 15,
                "url": "https://example.com/image.png"
                """,
                """
                "postImageId": 15,
                "style": "width: 100px"
                """,
                """
                "postImageId": 15,
                "publicId": "secret"
                """)) {
            Map<String, Object> document = imageDocument(attrs);

            assertThatThrownBy(() -> service.validate(document))
                    .isInstanceOf(InvalidPostContentDocumentException.class);
        }
    }

    @Test
    void rejectsContentInsideImageNode() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": { "postImageId": 15 },
                      "content": [{ "type": "text", "text": "No permitido" }]
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void rejectsUnknownMarks() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        { "type": "text", "text": "Texto", "marks": [{ "type": "code" }] }
                      ]
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void rejectsUnsafeLinks() throws Exception {
        Map<String, Object> document = documentWithLink("javascript:alert(1)");

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void rejectsDataLinks() throws Exception {
        Map<String, Object> document = documentWithLink("data:text/html;base64,PHNjcmlwdD4=");

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void rejectsArbitraryLinkAttributes() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "Link",
                          "marks": [
                            {
                              "type": "link",
                              "attrs": {
                                "href": "https://www.udea.edu.co",
                                "style": "color: red"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void buildsDocumentFromLegacyPlainText() {
        Map<String, Object> document = service.documentFromPlainText("Linea 1\nLinea 2");

        assertThat(service.extractPlainText(document)).isEqualTo("Linea 1\nLinea 2");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> json(String value) throws Exception {
        return jsonMapper.readValue(value, Map.class);
    }

    private Map<String, Object> documentWithLink(String href) throws Exception {
        return json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "Link",
                          "marks": [{ "type": "link", "attrs": { "href": "%s" } }]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(href));
    }

    private Map<String, Object> documentWithMark(String mark) throws Exception {
        return json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "Texto",
                          "marks": [%s]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(mark));
    }

    private Map<String, Object> imageDocument(String attrs) throws Exception {
        return json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": { %s }
                    }
                  ]
                }
                """.formatted(attrs));
    }

    private Map<String, Object> videoDocument(String attrs) throws Exception {
        return json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "video",
                      "attrs": { %s }
                    }
                  ]
                }
                """.formatted(attrs));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> linkAttrs(Map<String, Object> document) {
        return markAttrs(document);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> markAttrs(Map<String, Object> document) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) document.get("content");
        List<Map<String, Object>> paragraphContent = (List<Map<String, Object>>) content.get(0).get("content");
        List<Map<String, Object>> marks = (List<Map<String, Object>>) paragraphContent.get(0).get("marks");

        return (Map<String, Object>) marks.get(0).get("attrs");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> videoAttrs(Map<String, Object> document) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) document.get("content");

        return (Map<String, Object>) content.get(0).get("attrs");
    }
}
