package io.worldloom;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldDefinitionTest {
    private String example() throws Exception { return Files.readString(Path.of("../examples/ashen-empire.json")); }

    @Test void settingsAndUnknownCustomFieldsSurviveRoundTrip() throws Exception {
        var mapper = new ObjectMapper();
        for (String file : new String[]{"ashen-empire", "orbital-garden"}) {
            String source = Files.readString(Path.of("../examples/" + file + ".json"));
            assertEquals(mapper.readTree(source), mapper.readTree(WorldDefinition.parse(source).json()));
        }
    }

    @Test void rejectsUnsupportedVersionAndBlankBackground() throws Exception {
        String source = example();
        assertThrows(IllegalArgumentException.class, () -> WorldDefinition.parse(source.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")));
        var node = new ObjectMapper().readTree(source);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("background", " ");
        assertThrows(IllegalArgumentException.class, () -> WorldDefinition.parse(node.toString()));
    }

    @Test void rejectsDuplicateFactionIds() throws Exception {
        String source = example().replace("\"id\": \"collectors\"", "\"id\": \"keepers\"");
        assertThrows(IllegalArgumentException.class, () -> WorldDefinition.parse(source));
    }

    @Test void rejectsAmbiguousOrOversizedJson() throws Exception {
        assertThrows(java.io.IOException.class, () -> WorldDefinition.parse("{\"id\":\"a\",\"id\":\"b\"}"));
        String source = example();
        assertThrows(java.io.IOException.class, () -> WorldDefinition.parse(source + " {}"));
        assertThrows(IllegalArgumentException.class, () -> WorldDefinition.parse(source.replace("Political intrigue", "x".repeat(350_000))));
    }
}
