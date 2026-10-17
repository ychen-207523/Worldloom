package io.worldloom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

/** A validated document, preserving setting-specific fields without imposing game mechanics. */
public final class WorldDefinition {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final ObjectNode document;

    private WorldDefinition(ObjectNode document) { this.document = document.deepCopy(); }

    public static WorldDefinition parse(String source) throws IOException {
        JsonNode value = JSON.readTree(source);
        if (!(value instanceof ObjectNode root)) throw new IllegalArgumentException("World must be a JSON object");
        if (!root.path("schemaVersion").isIntegralNumber() || !root.path("schemaVersion").canConvertToInt()
                || root.path("schemaVersion").intValue() != 1)
            throw new IllegalArgumentException("schemaVersion must be 1");
        requiredText(root, "id");
        if (!root.path("id").asText().matches("[a-z0-9][a-z0-9-]{0,63}"))
            throw new IllegalArgumentException("id must be 1–64 lowercase letters, digits or hyphens, starting with a letter or digit");
        requiredText(root, "name");
        requiredText(root, "background");
        requiredText(root, "tone");
        for (String field : List.of("factions", "locations", "resources", "attributes")) {
            JsonNode entries = root.path(field);
            if (!entries.isArray()) throw new IllegalArgumentException(field + " must be an array (empty is allowed)");
            var ids = new HashSet<String>();
            for (JsonNode entry : entries) {
                if (!entry.isObject()) throw new IllegalArgumentException(field + " entries must be objects");
                requiredText(entry, "id");
                requiredText(entry, "name");
                if (!ids.add(entry.path("id").asText()))
                    throw new IllegalArgumentException("Duplicate id in " + field + ": " + entry.path("id").asText());
            }
        }
        JsonNode rules = root.path("rules");
        if (!rules.isArray()) throw new IllegalArgumentException("rules must be an array of descriptions");
        for (JsonNode rule : rules) {
            if (!rule.isTextual() || rule.asText().isBlank())
                throw new IllegalArgumentException("Each rule must be nonempty text");
        }
        WorldDefinition world = new WorldDefinition(root);
        // Leave headroom below DynamoDB's 400 KB item limit for keys and metadata.
        if (world.json().getBytes(StandardCharsets.UTF_8).length > 350_000)
            throw new IllegalArgumentException("World definition exceeds the 350 KB application limit");
        return world;
    }

    private static void requiredText(JsonNode node, String field) {
        if (!node.path(field).isTextual() || node.path(field).asText().isBlank())
            throw new IllegalArgumentException(field + " must be nonempty text");
    }

    public String id() { return document.path("id").asText(); }
    public String name() { return document.path("name").asText(); }
    public String json() { return document.toString(); }
    public String prettyJson() throws IOException { return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(document); }
}
