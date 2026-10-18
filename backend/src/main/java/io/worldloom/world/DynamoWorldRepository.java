package io.worldloom.world;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public final class DynamoWorldRepository implements WorldRepository {
    private final DynamoDbClient client;
    private final String table;

    public DynamoWorldRepository(DynamoDbClient client, String table) {
        if (table == null || table.isBlank()) throw new IllegalArgumentException("WORLDLOOM_TABLE is required");
        this.client = client;
        this.table = table;
    }

    public void create(WorldDefinition world) {
        client.putItem(PutItemRequest.builder().tableName(table)
                .item(Map.of("worldId", text(world.id()), "definition", text(world.json()),
                        "createdAt", text(Instant.now().toString())))
                .conditionExpression("attribute_not_exists(worldId)").build());
    }

    public Optional<WorldDefinition> find(String id) throws IOException {
        var item = client.getItem(GetItemRequest.builder().tableName(table)
                .key(Map.of("worldId", text(id))).consistentRead(true).build()).item();
        if (item.isEmpty()) return Optional.empty();
        var definition = item.get("definition");
        if (definition == null || definition.s() == null)
            throw new IllegalStateException("Saved world has no definition: " + id);
        WorldDefinition world = WorldDefinition.parse(definition.s());
        if (!world.id().equals(id)) throw new IllegalStateException("Saved world ID does not match its key");
        return Optional.of(world);
    }

    private static AttributeValue text(String value) { return AttributeValue.builder().s(value).build(); }
}
