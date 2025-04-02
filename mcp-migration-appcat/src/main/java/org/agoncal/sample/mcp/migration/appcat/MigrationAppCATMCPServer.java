package org.agoncal.sample.mcp.migration.appcat;

import io.quarkiverse.mcp.server.Content;
import io.quarkiverse.mcp.server.McpLog;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class MigrationAppCATMCPServer {

    private static final Logger log = Logger.getLogger(MigrationAppCATMCPServer.class);

    @Inject
    ResourceManager resourceManager;

    @Tool(name = "lists_all_the_available_migration_guides", description = "Lists all the available Java to Azure migration guides.")
    public ToolResponse listAvailableMigrationGuides(McpLog mcpLog) {
        log.info("Lists all the available Java to Azure migration guides");

        List<Content> content = new ArrayList<>();

        mcpLog.info("Returning " + content.size() + " migration guides");
        return ToolResponse.success();
    }

    @Resource(name = "doc_quickstart_postgresql_flexible_server", description = "In this quickstart, you learn how to create, update, and delete an Azure Database for PostgreSQL flexible server instance using the Azure SDK for Java. The code examples are written in Java and use the Azure SDK libraries to interact with the Azure Database for PostgreSQL flexible server service", uri = "https://learn.microsoft.com/en-us/azure/postgresql/flexible-server/quickstart-create-server-java-sdk", mimeType = "text/html")
    public TextResourceContents docQuickstartPostgresqlFlexibleServer() {
        return TextResourceContents.create("https://learn.microsoft.com/en-us/azure/postgresql/flexible-server/quickstart-create-server-java-sdk",
            """
                In this quickstart, you learn how to create, update, and delete an Azure Database for PostgreSQL flexible server instance using the Azure SDK for Java. The code examples are written in Java and use the Azure SDK libraries to interact with the Azure Database for PostgreSQL flexible server service.

                The Azure SDK for Java provides a set of libraries that allow you to interact with Azure services using Java. The SDK provides a consistent programming model and simplifies working with Azure services, including Azure Database for PostgreSQL flexible server.
                """);
    }
}
