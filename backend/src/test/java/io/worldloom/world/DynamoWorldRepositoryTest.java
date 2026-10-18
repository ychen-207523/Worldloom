package io.worldloom.world;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import static org.junit.jupiter.api.Assertions.*;

class DynamoWorldRepositoryTest {
    @Test void createsWithoutOverwriteAndLoadsFullDocument() throws Exception {
        var stored = new AtomicReference<Map<String, AttributeValue>>();
        var client = (DynamoDbClient) Proxy.newProxyInstance(DynamoDbClient.class.getClassLoader(),
                new Class<?>[]{DynamoDbClient.class}, (proxy, method, args) -> {
                    if (method.getName().equals("putItem")) {
                        var request = (PutItemRequest) args[0];
                        assertEquals("WorldLoomWorlds", request.tableName());
                        assertEquals("attribute_not_exists(worldId)", request.conditionExpression());
                        stored.set(request.item());
                        return PutItemResponse.builder().build();
                    }
                    if (method.getName().equals("getItem")) {
                        var request = (GetItemRequest) args[0];
                        assertTrue(request.consistentRead());
                        assertEquals("ashen-empire", request.key().get("worldId").s());
                        return GetItemResponse.builder().item(stored.get()).build();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        var repository = new DynamoWorldRepository(client, "WorldLoomWorlds");
        assertTrue(repository.find("ashen-empire").isEmpty());
        var world = WorldDefinition.parse(Files.readString(Path.of("../examples/ashen-empire.json")));
        repository.create(world);
        assertEquals(world.json(), repository.find(world.id()).orElseThrow().json());
        assertNotNull(stored.get().get("createdAt"));
    }
}
