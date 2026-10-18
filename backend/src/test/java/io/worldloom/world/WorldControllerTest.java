package io.worldloom.world;

import io.worldloom.api.ApiErrors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorldControllerTest {
    private TestRepository repository;
    private MockMvc api;
    private String sample;

    @BeforeEach void setup() throws Exception {
        repository = new TestRepository();
        api = MockMvcBuilders.standaloneSetup(new WorldController(repository))
                .setControllerAdvice(new ApiErrors()).build();
        sample = Files.readString(Path.of("../examples/ashen-empire.json"));
    }

    @Test void createsAndReadsWorldWithCustomFields() throws Exception {
        api.perform(post("/api/worlds").contentType("application/json").content(sample))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/worlds/ashen-empire"))
                .andExpect(jsonPath("$.custom.calendar.monthsPerYear").value(10));
        org.junit.jupiter.api.Assertions.assertEquals("ashen-empire", repository.saved.id());
        api.perform(get("/api/worlds/ashen-empire")).andExpect(status().isOk())
                .andExpect(jsonPath("$.custom.calendar.monthsPerYear").value(10));
    }

    @Test void rejectsInvalidWorldBeforeWriting() throws Exception {
        api.perform(post("/api/worlds").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        api.perform(post("/api/worlds").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(0, repository.calls);
    }

    @Test void reportsConflictWithoutOverwriting() throws Exception {
        repository.saved = WorldDefinition.parse(sample);
        api.perform(post("/api/worlds").contentType("application/json").content(sample))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
    }

    @Test void reportsMissingWorld() throws Exception {
        api.perform(get("/api/worlds/missing")).andExpect(status().isNotFound());
    }

    @Test void hidesAwsExceptionDetails() throws Exception {
        repository.failure = SdkClientException.create("sensitive internal detail");
        api.perform(get("/api/worlds/ashen-empire")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive"))));
    }

    private static class TestRepository implements WorldRepository {
        WorldDefinition saved;
        RuntimeException failure;
        int calls;

        public void create(WorldDefinition world) {
            calls++;
            if (failure != null) throw failure;
            if (saved != null && saved.id().equals(world.id()))
                throw ConditionalCheckFailedException.builder().message("duplicate").build();
            saved = world;
        }

        public Optional<WorldDefinition> find(String id) {
            calls++;
            if (failure != null) throw failure;
            return saved != null && saved.id().equals(id) ? Optional.of(saved) : Optional.empty();
        }
    }
}
