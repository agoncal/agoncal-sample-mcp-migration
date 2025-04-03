package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.McpLog;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
import org.jboss.logging.Logger;

import java.util.List;

public class MigrationOpenRewriteMCPServer {

    private static final Logger log = Logger.getLogger(MigrationOpenRewriteMCPServer.class);

    @Tool(name = "lists_all_the_openrewrite_recipes", description = "Lists all the available OpenRewrite recipes.")
    public ToolResponse listsAllOpenrewriteRecipes(McpLog mcpLog) {
        log.info("Lists all the available OpenRewrite recipes.");

        List<TextContent> recipes = List.of(
            new TextContent(new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.RemoveIllegalSemicolons", "Remove illegal semicolons", "Remove semicolons after package declarations and imports, no longer accepted in Java 21 as of [JDK-8027682](https://bugs.openjdk.org/browse/JDK-8027682).").toString()),
            new TextContent(new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.lang.ThreadStopUnsupported", "Replace `Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` with `throw new UnsupportedOperationException()`", "`Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` always throws a `new UnsupportedOperationException` in Java 21+. This recipe makes that explicit, as the migration is more complicated.See https").toString()),
            new TextContent(new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.net.URLConstructorToURICreate", "Convert `new URL(String)` to `URI.create(String).toURL()`", "Converts `new URL(String)` constructor to `URI.create(String).toURL()`.").toString()),
            new TextContent(new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.lang.ExplicitRecordImport", "Add explicit import for `Record` classes", "Add explicit import for `Record` classes when upgrading past Java 14+, to avoid conflicts with `java.lang.Record`.").toString())
        );

        mcpLog.info("Returning " + recipes.size() + " recipes");
        return ToolResponse.success(recipes);
    }

    @Resource(name = "lists_all_the_openrewrite_recipes",
        uri = "file:///Users/agoncal/Documents/recipes.json",
        mimeType = "application/json",
        description = "Lists all the available OpenRewrite recipes."
    )
    public TextResourceContents dataAllOpenrewriteRecipes() throws JsonProcessingException {

        List<Recipe> recipes = List.of(
            new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.RemoveIllegalSemicolons", "Remove illegal semicolons", "Remove semicolons after package declarations and imports, no longer accepted in Java 21 as of [JDK-8027682](https://bugs.openjdk.org/browse/JDK-8027682)."),
            new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.lang.ThreadStopUnsupported", "Replace `Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` with `throw new UnsupportedOperationException()`", "`Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` always throws a `new UnsupportedOperationException` in Java 21+. This recipe makes that explicit, as the migration is more complicated.See https"),
            new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.net.URLConstructorToURICreate", "Convert `new URL(String)` to `URI.create(String).toURL()`", "Converts `new URL(String)` constructor to `URI.create(String).toURL()`."),
            new Recipe("Migrate to Java 21", "org.openrewrite.java.migrate.lang.ExplicitRecordImport", "Add explicit import for `Record` classes", "Add explicit import for `Record` classes when upgrading past Java 14+, to avoid conflicts with `java.lang.Record`.")
        );

        ObjectMapper mapper = new ObjectMapper();

        return TextResourceContents.create("file:///Users/agoncal/Documents/recipes.json", mapper.writeValueAsString(recipes));
    }
}

record Recipe(String migration, String fqn, String name, String description) {
}

