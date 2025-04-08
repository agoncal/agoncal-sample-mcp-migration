package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@QuarkusTest
public class MigrationOpenRewriteMCPServerTest {

    @Test
    public void testGetRecipeAsJson() throws JsonProcessingException {
        // When
        String result = MigrationOpenRewriteMCPServer.getRecipeAsJson();

        // Then
        assertNotNull(result, "The JSON string should not be null");
        assertFalse(result.isEmpty(), "The JSON string should not be empty");
    }

    @Test
    public void testExecuteURLConstructorToURICreateRecipe() throws IOException {
        // When
        MigrationOpenRewriteMCPServer migrationOpenRewriteMCPServer = new MigrationOpenRewriteMCPServer();
        ToolResponse result = migrationOpenRewriteMCPServer.executeURLConstructorToURICreateRecipe();

        // Then
        assertNotNull(result, "The JSON string should not be null");
    }
}

