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

    @SuppressWarnings("unchecked")
    private Map<String, Object> linkAttrs(Map<String, Object> document) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) document.get("content");
        List<Map<String, Object>> paragraphContent = (List<Map<String, Object>>) content.get(0).get("content");
        List<Map<String, Object>> marks = (List<Map<String, Object>>) paragraphContent.get(0).get("marks");

        return (Map<String, Object>) marks.get(0).get("attrs");
    }
}
