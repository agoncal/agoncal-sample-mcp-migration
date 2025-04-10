package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.io.IOException;

@QuarkusTest
public class JavaMigrationMCPServerTest {

    @Inject
    JavaMigrationMCPServer openRewriteMCPServer;

    @Test
    public void testGetRecipeAsJson() throws JsonProcessingException {
        // When
        String result = openRewriteMCPServer.getRecipeAsJson();

        // Then
        assertNotNull(result, "The JSON string should not be null");
        assertFalse(result.isEmpty(), "The JSON string should not be empty");
    }

    @Test
    public void testExecuteURLConstructorToURICreateRecipe() throws IOException {
        ToolResponse result = openRewriteMCPServer.executeURLConstructorToURICreateRecipe();
        assertTrue(result.content().getFirst().toString().contains("made 2 changes in the code"));
    }

    @Test
    public void testExecuteCastArraysAsListToListRecipe() throws IOException {
        ToolResponse result = openRewriteMCPServer.executeCastArraysAsListToListRecipe();
        assertTrue(result.content().getFirst().toString().contains("made 1 changes in the code"));
    }

    @Test
    public void testExecuteThreadStopUnsupportedRecipe() throws IOException {
        ToolResponse result = openRewriteMCPServer.executeThreadStopUnsupportedRecipe();
        assertTrue(result.content().getFirst().toString().contains("made 1 changes in the code"));
    }

    @Test
    public void testRemoveTemporalAnnotationRecipe() throws IOException {
        ToolResponse result = openRewriteMCPServer.executeRemoveTemporalAnnotationRecipe();
        assertTrue(result.content().getFirst().toString().contains("made no change in the code"));
    }

    @Test
    public void testUseMapOfRecipeRecipe() throws IOException {
        ToolResponse result = openRewriteMCPServer.executeUseMapOfRecipe();
        assertTrue(result.content().getFirst().toString().contains("made no change in the code"));
    }
}

