package edu.udea.hidrologia.post.content;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Component
public class PostContentDocumentService {

    private static final Set<String> BLOCK_NODES = Set.of(
            "paragraph",
            "heading",
            "bulletList",
            "orderedList",
            "listItem",
            "blockquote",
            "academicBlock");
    private static final Set<String> INLINE_NODES = Set.of("text", "hardBreak");
    private static final Set<String> MARKS = Set.of(
            "bold",
            "italic",
            "underline",
            "link",
            "textSize",
            "textColor",
            "highlight");
    private static final Set<String> LINK_PROTOCOLS = Set.of("http", "https", "mailto");
    private static final Set<String> TEXT_ALIGNMENTS = Set.of("left", "center", "right", "justify");
    private static final Set<String> TEXT_SIZES = Set.of("small", "normal", "large");
    private static final Set<String> TEXT_COLORS = Set.of("default", "institutional", "blue", "muted", "danger");
    private static final Set<String> HIGHLIGHT_KINDS = Set.of("note", "important");
    private static final Set<String> ACADEMIC_BLOCK_KINDS = Set.of("note", "example", "important");

    private final JsonMapper jsonMapper;

    public PostContentDocumentService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Map<String, Object> emptyDocument() {
        ObjectNode document = JsonNodeFactory.instance.objectNode();
        document.put("type", "doc");
        ArrayNode content = document.putArray("content");
        content.add(JsonNodeFactory.instance.objectNode().put("type", "paragraph"));

        return toSerializableDocument(document);
    }

    public Map<String, Object> documentFromPlainText(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return emptyDocument();
        }

        ObjectNode document = JsonNodeFactory.instance.objectNode();
        document.put("type", "doc");
        ArrayNode content = document.putArray("content");
        for (String line : plainText.split("\\R", -1)) {
            ObjectNode paragraph = JsonNodeFactory.instance.objectNode();
            paragraph.put("type", "paragraph");
            if (!line.isEmpty()) {
                ArrayNode paragraphContent = paragraph.putArray("content");
                paragraphContent.add(JsonNodeFactory.instance.objectNode()
                        .put("type", "text")
                        .put("text", line));
            }
            content.add(paragraph);
        }

        return toSerializableDocument(document);
    }

    public Map<String, Object> validate(Map<String, Object> document) {
        return toSerializableDocument(validateJson(toJsonNode(document)));
    }

    public String extractPlainText(Map<String, Object> document) {
        JsonNode validated = validateJson(toJsonNode(document));
        StringBuilder text = new StringBuilder();
        appendNodeText(validated, text);

        return text.toString().strip();
    }

    public Map<String, Object> toSerializableDocument(Map<String, Object> document) {
        return validate(document);
    }

    public Map<String, Object> toDocument(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw invalid();
        }

        return toSerializableDocument(validateJson(jsonMapper.valueToTree(value)));
    }

    private JsonNode validateJson(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw invalid();
        }

        validateOnlyFields(document, Set.of("type", "content"));
        if (!"doc".equals(textValue(document.get("type")))) {
            throw invalid();
        }
        validateOptionalContent(document.get("content"), "doc");

        return canonicalizeNode(document);
    }

    private Map<String, Object> toSerializableDocument(JsonNode document) {
        JsonNode validated = validateJson(document);
        Object value = toSerializableValue(validated);
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, entry) -> result.put(String.valueOf(key), entry));

        return result;
    }

    private JsonNode toJsonNode(Map<String, Object> document) {
        if (document == null) {
            throw invalid();
        }

        return jsonMapper.valueToTree(document);
    }

    private void validateOptionalContent(JsonNode content, String parentType) {
        if (content == null) {
            return;
        }
        if (!content.isArray()) {
            throw invalid();
        }

        for (JsonNode child : content) {
            validateNode(child, parentType);
        }
    }

    private void validateNode(JsonNode node, String parentType) {
        if (node == null || !node.isObject()) {
            throw invalid();
        }

        String type = textValue(node.get("type"));
        if (type == null) {
            throw invalid();
        }

        if (BLOCK_NODES.contains(type)) {
            validateBlockNode(node, type, parentType);
            return;
        }

        if (INLINE_NODES.contains(type)) {
            validateInlineNode(node, type, parentType);
            return;
        }

        throw invalid();
    }

    private void validateBlockNode(JsonNode node, String type, String parentType) {
        validateParent(type, parentType);

        if ("heading".equals(type)) {
            validateOnlyFields(node, Set.of("type", "attrs", "content"));
            validateHeadingAttrs(node.get("attrs"));
        } else if ("paragraph".equals(type)) {
            validateOnlyFields(node, Set.of("type", "attrs", "content"));
            validateTextBlockAttrs(node.get("attrs"));
        } else if ("orderedList".equals(type)) {
            validateOnlyFields(node, Set.of("type", "attrs", "content"));
            validateOrderedListAttrs(node.get("attrs"));
        } else if ("academicBlock".equals(type)) {
            validateOnlyFields(node, Set.of("type", "attrs", "content"));
            validateAcademicBlockAttrs(node.get("attrs"));
        } else {
            validateOnlyFields(node, Set.of("type", "attrs", "content"));
            validateNoAttrs(node.get("attrs"));
        }

        validateOptionalContent(node.get("content"), type);
    }

    private void validateInlineNode(JsonNode node, String type, String parentType) {
        validateParent(type, parentType);
        if ("text".equals(type)) {
            validateOnlyFields(node, Set.of("type", "text", "marks"));
            JsonNode text = node.get("text");
            if (text == null || !text.isTextual()) {
                throw invalid();
            }
            validateMarks(node.get("marks"));
            return;
        }

        validateOnlyFields(node, Set.of("type"));
    }

    private void validateMarks(JsonNode marks) {
        if (marks == null) {
            return;
        }
        if (!marks.isArray()) {
            throw invalid();
        }
        for (JsonNode mark : marks) {
            if (mark == null || !mark.isObject()) {
                throw invalid();
            }
            String type = textValue(mark.get("type"));
            if (!MARKS.contains(type)) {
                throw invalid();
            }
            validateMark(mark, type);
        }
    }

    private void validateMark(JsonNode mark, String type) {
        switch (type) {
            case "link" -> {
                validateOnlyFields(mark, Set.of("type", "attrs"));
                validateLinkAttrs(mark.get("attrs"));
            }
            case "textSize" -> {
                validateOnlyFields(mark, Set.of("type", "attrs"));
                validateRequiredTokenAttr(mark.get("attrs"), "size", TEXT_SIZES);
            }
            case "textColor" -> {
                validateOnlyFields(mark, Set.of("type", "attrs"));
                validateRequiredTokenAttr(mark.get("attrs"), "color", TEXT_COLORS);
            }
            case "highlight" -> {
                validateOnlyFields(mark, Set.of("type", "attrs"));
                validateRequiredTokenAttr(mark.get("attrs"), "kind", HIGHLIGHT_KINDS);
            }
            default -> validateOnlyFields(mark, Set.of("type"));
        }
    }

    private void validateHeadingAttrs(JsonNode attrs) {
        if (attrs == null || !attrs.isObject()) {
            throw invalid();
        }
        validateOnlyFields(attrs, Set.of("level", "textAlign"));
        JsonNode level = attrs.get("level");
        if (level == null || !level.isIntegralNumber()) {
            throw invalid();
        }
        int value = level.intValue();
        if (value < 2 || value > 3) {
            throw invalid();
        }
        validateOptionalToken(attrs.get("textAlign"), TEXT_ALIGNMENTS);
    }

    private void validateTextBlockAttrs(JsonNode attrs) {
        if (attrs == null) {
            return;
        }
        if (!attrs.isObject()) {
            throw invalid();
        }
        validateOnlyFields(attrs, Set.of("textAlign"));
        validateOptionalToken(attrs.get("textAlign"), TEXT_ALIGNMENTS);
    }

    private void validateOrderedListAttrs(JsonNode attrs) {
        if (attrs == null) {
            return;
        }
        if (!attrs.isObject()) {
            throw invalid();
        }
        validateOnlyFields(attrs, Set.of("start", "type"));
        JsonNode start = attrs.get("start");
        if (start != null && (!start.isIntegralNumber() || start.intValue() < 1)) {
            throw invalid();
        }
        JsonNode listType = attrs.get("type");
        if (listType != null && !listType.isNull() && !listType.isTextual()) {
            throw invalid();
        }
    }

    private void validateAcademicBlockAttrs(JsonNode attrs) {
        validateRequiredTokenAttr(attrs, "kind", ACADEMIC_BLOCK_KINDS);
    }

    private void validateLinkAttrs(JsonNode attrs) {
        if (attrs == null || !attrs.isObject()) {
            throw invalid();
        }
        validateOnlyFields(attrs, Set.of("href", "target", "rel", "class", "title"));
        String href = textValue(attrs.get("href"));
        if (href == null || href.isBlank() || !isSafeLink(href)) {
            throw invalid();
        }
    }

    private void validateNoAttrs(JsonNode attrs) {
        if (attrs == null) {
            return;
        }
        if (!attrs.isObject() || attrs.size() > 0) {
            throw invalid();
        }
    }

    private void validateRequiredTokenAttr(JsonNode attrs, String field, Set<String> allowedValues) {
        if (attrs == null || !attrs.isObject()) {
            throw invalid();
        }
        validateOnlyFields(attrs, Set.of(field));
        JsonNode value = attrs.get(field);
        if (!isAllowedToken(value, allowedValues)) {
            throw invalid();
        }
    }

    private void validateOptionalToken(JsonNode value, Set<String> allowedValues) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!isAllowedToken(value, allowedValues)) {
            throw invalid();
        }
    }

    private boolean isAllowedToken(JsonNode value, Set<String> allowedValues) {
        return value != null && value.isTextual() && allowedValues.contains(value.asText());
    }

    private void validateOnlyFields(JsonNode node, Set<String> allowedFields) {
        for (String field : node.propertyNames()) {
            if (!allowedFields.contains(field)) {
                throw invalid();
            }
        }
    }

    private void validateParent(String type, String parentType) {
        if ("doc".equals(parentType)) {
            if (!BLOCK_NODES.contains(type) || "listItem".equals(type)) {
                throw invalid();
            }
            return;
        }

        if ("bulletList".equals(parentType) || "orderedList".equals(parentType)) {
            if (!"listItem".equals(type)) {
                throw invalid();
            }
            return;
        }

        if ("paragraph".equals(parentType) || "heading".equals(parentType) || "inline".equals(parentType)) {
            if (!INLINE_NODES.contains(type)) {
                throw invalid();
            }
            return;
        }

        if ("blockquote".equals(parentType) || "listItem".equals(parentType)) {
            if (!BLOCK_NODES.contains(type) || "listItem".equals(type)) {
                throw invalid();
            }
            return;
        }

        if ("academicBlock".equals(parentType)) {
            if (!BLOCK_NODES.contains(type) || "listItem".equals(type) || "academicBlock".equals(type)) {
                throw invalid();
            }
            return;
        }

        throw invalid();
    }

    private void appendNodeText(JsonNode node, StringBuilder text) {
        String type = textValue(node.get("type"));
        if ("text".equals(type)) {
            text.append(node.get("text").asText());
            return;
        }
        if ("hardBreak".equals(type)) {
            text.append('\n');
            return;
        }

        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) {
                appendNodeText(child, text);
            }
        }

        if (BLOCK_NODES.contains(type)) {
            text.append('\n');
        }
    }

    private boolean isSafeLink(String href) {
        if (href.startsWith("/") && !href.startsWith("//")) {
            return true;
        }
        if (href.startsWith("#")) {
            return true;
        }

        try {
            URI uri = new URI(href);
            String scheme = uri.getScheme();
            return scheme != null && LINK_PROTOCOLS.contains(scheme.toLowerCase());
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private String textValue(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private JsonNode canonicalizeNode(JsonNode node) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        String type = textValue(node.get("type"));
        result.put("type", type);

        if ("text".equals(type)) {
            result.put("text", node.get("text").asText());
            JsonNode marks = canonicalizeMarks(node.get("marks"));
            if (marks != null) {
                result.set("marks", marks);
            }
            return result;
        }

        JsonNode attrs = canonicalizeAttrs(type, node.get("attrs"));
        if (attrs != null) {
            result.set("attrs", attrs);
        }

        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            ArrayNode canonicalContent = result.putArray("content");
            for (JsonNode child : content) {
                canonicalContent.add(canonicalizeNode(child));
            }
        }

        return result;
    }

    private JsonNode canonicalizeAttrs(String type, JsonNode attrs) {
        if ("heading".equals(type)) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            result.put("level", attrs.get("level").intValue());
            appendTextAlign(result, attrs);

            return result;
        }

        if ("paragraph".equals(type) && hasTextAlign(attrs)) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            appendTextAlign(result, attrs);

            return result;
        }

        if ("orderedList".equals(type) && attrs != null && attrs.isObject() && attrs.size() > 0) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            JsonNode start = attrs.get("start");
            if (start != null) {
                result.put("start", start.intValue());
            }
            JsonNode listType = attrs.get("type");
            if (listType != null) {
                if (listType.isNull()) {
                    result.set("type", JsonNodeFactory.instance.nullNode());
                } else {
                    result.put("type", listType.asText());
                }
            }

            return result;
        }

        if ("academicBlock".equals(type)) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            result.put("kind", attrs.get("kind").asText());

            return result;
        }

        return null;
    }

    private void appendTextAlign(ObjectNode result, JsonNode attrs) {
        JsonNode textAlign = attrs.get("textAlign");
        if (textAlign != null && !textAlign.isNull()) {
            result.put("textAlign", textAlign.asText());
        }
    }

    private boolean hasTextAlign(JsonNode attrs) {
        JsonNode textAlign = attrs == null ? null : attrs.get("textAlign");

        return textAlign != null && !textAlign.isNull();
    }

    private JsonNode canonicalizeMarks(JsonNode marks) {
        if (marks == null || !marks.isArray() || marks.isEmpty()) {
            return null;
        }

        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        for (JsonNode mark : marks) {
            String type = textValue(mark.get("type"));
            ObjectNode canonicalMark = JsonNodeFactory.instance.objectNode();
            canonicalMark.put("type", type);
            if ("link".equals(type)) {
                ObjectNode attrs = JsonNodeFactory.instance.objectNode();
                attrs.put("href", mark.get("attrs").get("href").asText());
                canonicalMark.set("attrs", attrs);
            } else if ("textSize".equals(type)) {
                ObjectNode attrs = JsonNodeFactory.instance.objectNode();
                attrs.put("size", mark.get("attrs").get("size").asText());
                canonicalMark.set("attrs", attrs);
            } else if ("textColor".equals(type)) {
                ObjectNode attrs = JsonNodeFactory.instance.objectNode();
                attrs.put("color", mark.get("attrs").get("color").asText());
                canonicalMark.set("attrs", attrs);
            } else if ("highlight".equals(type)) {
                ObjectNode attrs = JsonNodeFactory.instance.objectNode();
                attrs.put("kind", mark.get("attrs").get("kind").asText());
                canonicalMark.set("attrs", attrs);
            }
            result.add(canonicalMark);
        }

        return result;
    }

    private Object toSerializableValue(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            node.properties().forEach(entry -> result.put(entry.getKey(), toSerializableValue(entry.getValue())));

            return result;
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            node.forEach(item -> result.add(toSerializableValue(item)));

            return result;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNull()) {
            return null;
        }

        throw invalid();
    }

    private InvalidPostContentDocumentException invalid() {
        return new InvalidPostContentDocumentException("Post content document is invalid");
    }
}
