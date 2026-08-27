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
                  "content": [{ "type": "image" }]
                }
                """);

        assertThatThrownBy(() -> service.validate(document))
                .isInstanceOf(InvalidPostContentDocumentException.class);
    }

    @Test
    void detectsFutureImageReferencesWithoutAllowingImageNodesYet() throws Exception {
        Map<String, Object> document = json("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": { "postImageId": 15 }
                    }
                  ]
                }
                """);

        assertThat(service.referencesPostImageId(document, 15L)).isTrue();
        assertThat(service.referencesPostImageId(document, 16L)).isFalse();
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
}
