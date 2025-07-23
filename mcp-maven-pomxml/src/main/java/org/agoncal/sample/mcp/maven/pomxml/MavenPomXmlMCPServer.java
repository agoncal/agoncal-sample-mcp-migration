package org.agoncal.sample.mcp.maven.pomxml;

import io.quarkiverse.mcp.server.Content;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.Tool.Annotations;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.annotation.PostConstruct;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MavenPomXmlMCPServer {

    private static final Logger log = Logger.getLogger(MavenPomXmlMCPServer.class);
    private static final String DEFAULT_POM_XML_PATH = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration/mcp-maven-pomxml/src/test/resources/pomee6.xml";
    private static final Path POM_XML_PATH = Paths.get(
        Optional.ofNullable(System.getenv("POM_XML_PATH")).orElse(DEFAULT_POM_XML_PATH)
    ).toAbsolutePath();
    private static final MavenXpp3Reader reader = new MavenXpp3Reader();
    private static final MavenXpp3Writer writer = new MavenXpp3Writer();

    @PostConstruct
    void getPomXMLFile() throws IOException {
        InputStream inputStream = Files.newInputStream(POM_XML_PATH);
        if (inputStream == null) {
            throw new IOException("pom.xml not found in classpath");
        }
    }

    @Tool(name = "gets_all_the_properties", description = """
        This method returns all existing properties from a Maven pom.xml file. This method parses the XML structure of the POM file, locates the <properties> section, and retrieves all property name-value pairs defined within it.

        Example usage:
        - Input: pom.xml file containing properties like <java.version>11</java.version>, <maven.compiler.source>11</maven.compiler.source>
        - Output: Collection containing key-value pairs: {"java.version": "11", "maven.compiler.source": "11"}

        The method handles XML parsing automatically and returns an empty collection if no <properties> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllProperties() throws IOException, XmlPullParserException {
        log.info("gets all the properties");

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getProperties().isEmpty()) {
            return ToolResponse.success("No properties found in the pom.xml file.");
        }

        // Builds the list of properties
        List<Content> properties = model.getProperties().entrySet().stream()
            .map(entry -> new TextContent(new Pair((String) entry.getKey(), (String) entry.getValue()).toString()))
            .collect(Collectors.toList());

        return ToolResponse.success(properties);
    }

    @Tool(name = "adds_a_new_property", description = """
        This method adds a new property to an existing Maven pom.xml file. This method takes a property and inserts them into the <properties> section of the POM file. If no <properties> section exists, the method creates one. The method preserves the existing XML structure and formatting while safely adding the new property without overwriting existing properties or corrupting the file structure.

        Example usage:
        - Input: property name "java.version", value "11"
        - Result: Adds <java.version>11</java.version> to the <properties> section of the pom.xml

        The method handles XML parsing, property insertion, and file writing operations automatically.
        """,
        annotations = @Annotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse addNewProperty(
        @ToolArg(name = "property key", description = "The name of the property key to be added.") String key,
        @ToolArg(name = "property value", description = "The value of property to be added.") String value)
        throws IOException, XmlPullParserException {
        log.info("adds the new property " + key + " with value" + value);

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getProperties().containsKey(key)) {
            return ToolResponse.error("Property '" + key + "' already exist in the pom.xml file.");
        }

        // Adds a new property
        model.addProperty(key, value);

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The new property " + key + " has been added with value " + value);
    }

    @Tool(name = "updates_the_value_of_an_existing_property", description = """
        This method modifies the value of an existing property in a Maven pom.xml file. This method takes a property key (name) and a new value as parameters, locates the specified property within the <properties> section of the POM file, and updates its value while preserving all other properties and the XML structure. The method only updates existing properties and does not create new ones if the specified key is not found.

        Example usage:
        - Input: property key "java.version", new value "17"
        - Before: <java.version>11</java.version>
        - After: <java.version>17</java.version>

        The method handles XML parsing, property location, value replacement, and file writing operations automatically. If the specified property key does not exist in the pom.xml, the method typically returns an error or indication that the property was not found, without modifying the file.
        """,
        annotations = @Annotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingPropertyValue(
        @ToolArg(name = "property key", description = "The name of the property key to look for.") String key,
        @ToolArg(name = "property value", description = "The new value of the existing property.") String value)
        throws IOException, XmlPullParserException {
        log.info("updates the existing property " + key + " with the new value" + value);

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (!model.getProperties().containsKey(key)) {
            return ToolResponse.error("Property '" + key + "' does not exist in the pom.xml file.");
        }

        // Updates the property value
        model.getProperties().put(key, value);

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The value of the existing property " + key + " has been updated to " + value);
    }

    @Tool(name = "removes_an_existing_property", description = """
        This method deletes an existing property from a Maven pom.xml file. This method takes a property key (name) as a parameter, locates the specified property within the <properties> section of the POM file, and removes it entirely while preserving all other properties and the XML structure. The method only removes existing properties and does not modify the file if the specified key is not found.

        Example usage:
        - Input: property key "java.version"
        - Before: <properties><java.version>11</java.version><maven.compiler.source>11</maven.compiler.source></properties>
        - After: <properties><maven.compiler.source>11</maven.compiler.source></properties>

        The method handles XML parsing, property location, element removal, and file writing operations automatically. If the specified property key does not exist in the pom.xml, the method typically returns an error or indication that the property was not found, without modifying the file. If removing the property results in an empty <properties> section, the implementation may choose to either keep the empty section or remove it entirely.
        """,
        annotations = @Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingProperty(
        @ToolArg(name = "property key", description = "The name of the property key to remove.") String key)
        throws IOException, XmlPullParserException {
        log.info("remove the existing property " + key);

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (!model.getProperties().containsKey(key)) {
            return ToolResponse.error("Property '" + key + "' does not exist in the pom.xml file.");
        }

        // Removes the existing property
        model.getProperties().remove(key);

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The existing property " + key + " has been removed");
    }

    private static Model readModel() throws IOException, XmlPullParserException {
        Model model;
        try (InputStreamReader inputStreamReader = new InputStreamReader(Files.newInputStream(POM_XML_PATH))) {
            model = reader.read(inputStreamReader);
        }
        return model;
    }

    private static void writeModel(Model model) throws IOException, XmlPullParserException {
        try (OutputStream outputStream = Files.newOutputStream(POM_XML_PATH)) {
            writer.write(outputStream, model);
        }
    }
}

record Pair(String key, String value) {
}

