package org.agoncal.sample.mcp.migration.openrewrite;

import io.quarkiverse.mcp.server.BlobResourceContents;
import io.quarkiverse.mcp.server.Content;
import io.quarkiverse.mcp.server.McpLog;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceResponse;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MigrationOpenRewriteMCPServer {

    private static final Logger log = Logger.getLogger(MigrationOpenRewriteMCPServer.class);

    @Inject
    ResourceManager resourceManager;

    @Tool(name = "lists_all_the_available_migration_guides", description = "Lists all the available Java to Azure migration guides.")
    public ToolResponse listAvailableMigrationGuides(McpLog mcpLog) {
        log.info("Lists all the available Java to Azure migration guides");

        List<Content> content = new ArrayList<>();

        content.add(new TextContent(new Guide("azure-developer-java-migration-springboot-to-aca.pdf", "This guide describes what you should be aware of when you want to migrate an existing Spring Boot application to run on Azure Container Apps.", "azure-developer-java-migration-springboot-to-aca.pdf").toString()));

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

    public void addPdfMigrationGuides() {
        resourceManager.newResource("file:///project/alpha")
            .setUri("azure-developer-java-migration-springboot-to-aca.pdf")
            .setDescription("This guide describes what you should be aware of when you want to migrate an existing Spring Boot application to run on Azure Container Apps.")
            .setMimeType("application/pdf")
            .setHandler(
                args -> new ResourceResponse(
                    List.of(BlobResourceContents.create(args.requestUri().value(), new byte[0] /*Files.readAllBytes(Paths.ALPHA)*/))))
            .register();
    }
}

record Guide(String pdfName, String description, URI uri) {
    public Guide(String pdfName, String description, String uri) {
        this(pdfName, description, URI.create(uri));
    }
}

