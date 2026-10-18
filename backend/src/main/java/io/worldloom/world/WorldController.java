package io.worldloom.world;

import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class WorldController {
    private final WorldRepository repository;

    public WorldController(WorldRepository repository) { this.repository = repository; }

    @PostMapping(value = "/worlds", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(@RequestBody String body) {
        WorldDefinition world;
        try { world = WorldDefinition.parse(body); }
        catch (IOException e) { throw new IllegalArgumentException("World must contain valid JSON"); }
        repository.create(world);
        return ResponseEntity.created(URI.create("/api/worlds/" + world.id()))
                .contentType(MediaType.APPLICATION_JSON).body(world.json());
    }

    @GetMapping(value = "/worlds/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> find(@PathVariable String id) {
        if (!id.matches("[a-z0-9][a-z0-9-]{0,63}"))
            throw new IllegalArgumentException("Enter a valid world ID: lowercase letters, digits, and hyphens");
        try {
            var world = repository.find(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "No world found with ID: " + id));
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(world.json());
        } catch (IOException e) {
            throw new IllegalStateException("Stored world could not be read", e);
        }
    }
}
