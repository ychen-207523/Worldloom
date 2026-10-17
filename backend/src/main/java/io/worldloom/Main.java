package io.worldloom;

import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

public final class Main {
    public static void main(String[] args) {
        try { run(args); }
        catch (ConditionalCheckFailedException e) {
            System.err.println("World already exists. Choose a new id; existing worlds are never overwritten by create.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("WorldLoom: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        if (args.length != 2 || !(args[0].equals("validate") || args[0].equals("create") || args[0].equals("show")))
            throw new IllegalArgumentException("Usage: validate <world.json> | create <world.json> | show <world-id>");
        WorldDefinition world = args[0].equals("show") ? null
                : WorldDefinition.parse(Files.readString(Path.of(args[1])));
        if (args[0].equals("validate")) {
            System.out.println("Valid world: " + world.name() + " (" + world.id() + ")");
            return;
        }
        String table = System.getenv("WORLDLOOM_TABLE");
        if (table == null || table.isBlank()) throw new IllegalArgumentException("Set WORLDLOOM_TABLE to your AWS DynamoDB table name");
        try (DynamoDbClient client = DynamoDbClient.create()) {
            var repository = new DynamoWorldRepository(client, table);
            if (args[0].equals("create")) {
                repository.create(world);
                System.out.println("Created world: " + world.id());
            } else {
                System.out.println(repository.find(args[1])
                        .orElseThrow(() -> new IllegalArgumentException("World not found: " + args[1])).prettyJson());
            }
        }
    }
}
