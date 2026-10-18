package io.worldloom.world;

import java.io.IOException;
import java.util.Optional;

public interface WorldRepository {
    void create(WorldDefinition world);
    Optional<WorldDefinition> find(String id) throws IOException;
}
