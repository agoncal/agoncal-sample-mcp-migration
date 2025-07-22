package org.agoncal.sample.mcp.maven.pomxml;

import io.quarkiverse.mcp.server.Content;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
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
import java.util.stream.Collectors;

public class MavenPomXmlMCPServer {

    private static final Logger log = Logger.getLogger(MavenPomXmlMCPServer.class);
    private static final Path POM_XML_PATH = Paths.get(System.getenv("POM_XML_PATH")).toAbsolutePath();
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
        """)
    public ToolResponse getAllProperties() throws IOException, XmlPullParserException {
        log.info("gets all the properties");

        Model model = readModel();

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
        """)
    public ToolResponse addNewProperty(
        @ToolArg(name = "property key", description = "The name of the property key to be added.") String key,
        @ToolArg(name = "property value", description = "The value of property to be added.") String value)
        throws IOException, XmlPullParserException {
        log.info("adds the new property " + key + " with value" + value);

        Model model = readModel();
        model.addProperty(key, value);
        writeModel(model);

        return ToolResponse.success();
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

