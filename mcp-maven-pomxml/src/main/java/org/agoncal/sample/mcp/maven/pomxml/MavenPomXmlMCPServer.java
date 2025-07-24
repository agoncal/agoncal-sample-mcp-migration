package org.agoncal.sample.mcp.maven.pomxml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.Tool.Annotations;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.annotation.PostConstruct;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
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
    private static final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @PostConstruct
    void getPomXMLFile() throws IOException {
        InputStream inputStream = Files.newInputStream(POM_XML_PATH);
        if (inputStream == null) {
            throw new IOException("pom.xml not found in classpath");
        }
    }

    @Tool(name = "gets_all_the_profiles", description = """
        This method returns all existing profiles from a Maven pom.xml file. This method parses the XML structure of the POM file, locates the <profiles> section, and retrieves all profiles defined within it.

        Example usage:
        - Input: pom.xml file containing profiles like <profiles><profile><id>jakarta-ee</id></profile><profile><id>jacoco</id></profile></profiles>
        - Output: Collection containing: {"id": "jakarta-ee", "id": "jacoco"}

        The method handles XML parsing automatically and returns an empty collection if no <profiles> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(title = "gets all the profiles", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllProfiles() throws IOException, XmlPullParserException {
        log.info("gets all the profiles");

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getProfiles().isEmpty()) {
            return ToolResponse.success("No profiles in the pom.xml file.");
        }

        // Builds the list of dependencies
        List<ProfileRecord> profiles = model.getProfiles().stream()
            .map(profile -> new ProfileRecord(
                profile.getId()))
            .collect(Collectors.toList());

        return ToolResponse.success(toJson(profiles));
    }

    @Tool(name = "gets_all_the_plugins", description = """
        This method returns all existing plugins from a Maven pom.xml file. This method parses the XML structure of the POM file, locates the <plugins> section, and retrieves all plugin defined within it.

        Example usage:
        - Input: pom.xml file containing plugins like <plugin><artifactId>maven-compiler-plugin</artifactId><version>${version.maven.compiler.plugin}</version><inherited>true</inherited></plugin>
        - Output: Collection containing: {"artifactId": "maven-compiler-plugin", "version": "${version.maven.compiler.plugin}", "inherited": "true"}

        The method handles XML parsing automatically and returns an empty collection if no <plugins> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(title = "gets all the plugins", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllPlugins() throws IOException, XmlPullParserException {
        log.info("gets all the plugins");

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getBuild() == null) {
            return ToolResponse.success("No build section in the pom.xml file.");
        }
        if (model.getBuild().getPlugins().isEmpty()) {
            return ToolResponse.success("No plugins in the pom.xml file.");
        }

        // Builds the list of plugins PluginRecord from model.getBuild().getPlugins(). It also retrieves the dependencies of each plugin if they exist.
        List<PluginRecord> plugins = model.getBuild().getPlugins().stream()
            .map(plugin -> {
                List<DependencyRecord> dependencies = plugin.getDependencies() != null ? plugin.getDependencies().stream()
                    .map(dependency -> new DependencyRecord(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        dependency.getType(),
                        dependency.getScope()))
                    .collect(Collectors.toList()) : List.of();
                return new PluginRecord(
                    plugin.getGroupId(),
                    plugin.getArtifactId(),
                    plugin.getVersion(),
                    String.valueOf(plugin.isInherited()),
                    dependencies);
            })
            .collect(Collectors.toList());

        return ToolResponse.success(toJson(plugins));
    }

    @Tool(name = "updates_the_version_of_an_existing_plugin", description = """
        This method modifies the version of an existing plugin in a Maven pom.xml file. This method takes a plugin (groupId and artifactId) and a new version as parameters, locates the specified plugin within the <plugins> section of the POM file, and updates its value while preserving all other plugins and the XML structure. The method only updates existing plugins and does not create new ones if the specified groupId and artifactId are not found.

        Example usage:
        - Input: plugin "rg.apache.tomee.maven", "tomee-maven-plugin", "10.0.0-M1"
        - Before: <plugin><groupId>rg.apache.tomee.maven</groupId><artifactId>tomee-maven-plugin</artifactId><version>10.0.0-Beta1</version></plugin>
        - After: <plugin><groupId>rg.apache.tomee.maven</groupId><artifactId>tomee-maven-plugin</artifactId><version>10.0.0-M1</version></plugin>

        The method handles XML parsing, plugin location, value replacement, and file writing operations automatically. If the specified plugin key does not exist in the pom.xml, the method typically returns an error or indication that the plugin was not found, without modifying the file.
        """,
        annotations = @Annotations(title = "updates the version of an existing plugin", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingPluginVersion(
        @ToolArg(name = "group id", description = "The group id of the plugin key to be updated.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the plugin key to be updated.") String artifactId,
        @ToolArg(name = "version", description = "The new version of the plugin to be updated.") String version)
        throws IOException, XmlPullParserException {
        log.info("updates the existing plugin " + groupId + " " + artifactId + " " + version);

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getBuild() == null) {
            return ToolResponse.success("No build section in the pom.xml file.");
        }

        boolean found = false;
        for (Plugin plugin : model.getBuild().getPlugins()) {
            if (plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId)) {
                // Updates the plugin value
                plugin.setVersion(version);
                found = true;
                break;
            }
        }

        // If not found, returns an error
        if (!found) {
            return ToolResponse.error("Plugin '" + groupId + ":" + artifactId + "' not found in the pom.xml file.");
        }

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The version of the existing plugin " + groupId + " " + artifactId + " has been updated to " + version);
    }

    @Tool(name = "removes_an_existing_plugin", description = """
        This method deletes an existing plugin from a Maven pom.xml file. This method takes a plugin groupId and artifactId as a parameter, locates the specified plugin within the <plugins> section of the POM file, and removes it entirely while preserving all other plugins and the XML structure. The method only removes existing plugins and does not modify the file if the specified key is not found.

        Example usage:
        - Input: groupId "org.hibernate.orm", artifactId "hibernate-core"
        - Before: <plugins><plugin><groupId>org.hibernate.orm</groupId><artifactId>hibernate-core</artifactId><version>6.0.9.Final</version></plugin><plugin><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></plugin></plugins>
        - After: <plugins><plugin><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></plugin></plugins>

        The method handles XML parsing, plugin location, element removal, and file writing operations automatically. If the specified plugin does not exist in the pom.xml, the method typically returns an error or indication that the plugin was not found, without modifying the file. If removing the plugin results in an empty <plugins> section, the implementation may choose to either keep the empty section or remove it entirely.
        """,
        annotations = @Annotations(title = "removes an existing plugin", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingPlugin(
        @ToolArg(name = "group id", description = "The group id of the plugin to be removed.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the plugin to be removed.") String artifactId)
        throws IOException, XmlPullParserException {
        log.info("remove the existing plugin " + groupId + " " + artifactId);

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getBuild() == null) {
            return ToolResponse.success("No build section in the pom.xml file.");
        }

        // Removes the existing plugin
        boolean found = false;
        for (Plugin plugin : model.getBuild().getPlugins()) {
            if (plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId)) {
                model.getBuild().removePlugin(plugin);
                found = true;
                break;
            }
        }

        // If not found, returns an error
        if (!found) {
            return ToolResponse.error("Plugin '" + groupId + ":" + artifactId + "' not found in the pom.xml file.");
        }

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The existing plugin '" + groupId + ":" + artifactId + "' has been removed");
    }

    @Tool(name = "gets_all_the_dependencies", description = """
        This method returns all existing dependencies from a Maven pom.xml file. This method parses the XML structure of the POM file, locates the <dependencies> section, and retrieves all dependency defined within it.

        Example usage:
        - Input: pom.xml file containing dependencies like <dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version><scope>provided</scope></dependency>
        - Output: Collection containing: {"groupId": "jakarta.platform", "artifactId": "jakarta.jakartaee-api", "version": "${version.jakarta.ee}", "scope": "provided"}

        The method handles XML parsing automatically and returns an empty collection if no <dependencies> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(title = "gets all the dependencies", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllDependencies() throws IOException, XmlPullParserException {
        log.info("gets all the dependencies");

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getDependencies().isEmpty()) {
            return ToolResponse.success("No dependencies in the pom.xml file.");
        }

        // Builds the list of dependencies
        List<DependencyRecord> dependencies = model.getDependencies().stream()
            .map(dependency -> new DependencyRecord(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getType(),
                dependency.getScope()))
            .collect(Collectors.toList());

        return ToolResponse.success(toJson(dependencies));
    }

    @Tool(name = "adds_a_new_dependency", description = """
        This method adds a new dependency to an existing Maven pom.xml file. This method takes a dependency and inserts them into the <dependencies> section of the POM file. If no <dependencies> section exists, the method creates one. The method preserves the existing XML structure and formatting while safely adding the new dependency without overwriting existing dependencies or corrupting the file structure.

        Example usage:
        - Input: dependency name "jakarta.platform", "jakarta.jakartaee-api", "${version.jakarta.ee}", "provided"
        - Result: Adds <dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version><scope>provided</scope></dependency> to the <dependencies> section of the pom.xml

        The method handles XML parsing, dependency insertion, and file writing operations automatically.
        """,
        annotations = @Annotations(title = "adds a new dependency", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse addNewDependency(
        @ToolArg(name = "group id", description = "The group id of the dependency key to be added.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency key to be added.") String artifactId,
        @ToolArg(name = "version", description = "The version of the dependency key to be added.") String version,
        @ToolArg(name = "type", description = "The type of the dependency key to be added. Can be jar, pom. The default is jar so you don't need to add <type>jar</type>") String type,
        @ToolArg(name = "scope", description = "The scope of the dependency key to be added. Can be compile, provided, runtime, test, system, import. The default is compile so you don't need to add <scope>compile</scope>") String scope)
        throws IOException, XmlPullParserException {
        log.info("adds the new dependency " + groupId + " " + artifactId + " " + version + " " + type + " " + scope);

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                return ToolResponse.error("Dependency '" + groupId + ":" + artifactId + "' already exists in the pom.xml file.");
            }
        }

        // Adds a new dependency
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType(type);
        dependency.setScope(scope);
        model.addDependency(dependency);

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The new dependency " + groupId + " " + artifactId + " " + version + " has been added with value " + dependency);
    }

    @Tool(name = "updates_the_version_of_an_existing_dependency", description = """
        This method modifies the version of an existing dependency in a Maven pom.xml file. This method takes a dependency (groupId and artifactId) and a new version as parameters, locates the specified dependency within the <dependencies> section of the POM file, and updates its value while preserving all other dependencies and the XML structure. The method only updates existing dependencies and does not create new ones if the specified groupId and artifactId are not found.

        Example usage:
        - Input: dependency "jakarta.platform", "jakarta.jakartaee-api", "11"
        - Before: <dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></dependency>
        - After: <dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>11</version></dependency>

        The method handles XML parsing, dependency location, value replacement, and file writing operations automatically. If the specified dependency key does not exist in the pom.xml, the method typically returns an error or indication that the dependency was not found, without modifying the file.
        """,
        annotations = @Annotations(title = "updates the version of an existing dependency", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingDependencyVersion(
        @ToolArg(name = "group id", description = "The group id of the dependency key to be updated.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency key to be updated.") String artifactId,
        @ToolArg(name = "version", description = "The new version of the dependency to be updated.") String version)
        throws IOException, XmlPullParserException {
        log.info("updates the existing dependency " + groupId + " " + artifactId + " " + version);

        // Read the pom.xml file
        Model model = readModel();

        boolean found = false;
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                // Updates the dependency value
                dependency.setVersion(version);
                found = true;
                break;
            }
        }

        // If not found, returns an error
        if (!found) {
            return ToolResponse.error("Dependency '" + groupId + ":" + artifactId + "' not found in the pom.xml file.");
        }

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The version of the existing dependency " + groupId + " " + artifactId + " has been updated to " + version);
    }

    @Tool(name = "removes_an_existing_dependency", description = """
        This method deletes an existing dependency from a Maven pom.xml file. This method takes a dependency groupId and artifactId as a parameter, locates the specified dependency within the <dependencies> section of the POM file, and removes it entirely while preserving all other dependencies and the XML structure. The method only removes existing dependencies and does not modify the file if the specified key is not found.

        Example usage:
        - Input: groupId "org.hibernate.orm", artifactId "hibernate-core"
        - Before: <dependencies><dependency><groupId>org.hibernate.orm</groupId><artifactId>hibernate-core</artifactId><version>6.0.9.Final</version></dependency><dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></dependency></dependencies>
        - After: <dependencies><dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></dependency></dependencies>

        The method handles XML parsing, dependency location, element removal, and file writing operations automatically. If the specified dependency does not exist in the pom.xml, the method typically returns an error or indication that the dependency was not found, without modifying the file. If removing the dependency results in an empty <dependencies> section, the implementation may choose to either keep the empty section or remove it entirely.
        """,
        annotations = @Annotations(title = "removes an existing dependency", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingDependency(
        @ToolArg(name = "group id", description = "The group id of the dependency to be removed.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency to be removed.") String artifactId)
        throws IOException, XmlPullParserException {
        log.info("remove the existing dependency " + groupId + " " + artifactId);

        // Read the pom.xml file
        Model model = readModel();

        // Removes the existing dependency
        boolean found = false;
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                model.removeDependency(dependency);
                found = true;
                break;
            }
        }

        // If not found, returns an error
        if (!found) {
            return ToolResponse.error("Dependency '" + groupId + ":" + artifactId + "' not found in the pom.xml file.");
        }

        // Writes back the pom.xml file
        writeModel(model);

        return ToolResponse.success("The existing dependency '" + groupId + ":" + artifactId + "' has been removed");
    }

    @Tool(name = "gets_all_the_dependency_management_dependencies", description = """
        This method returns all existing dependencies of a <dependencyManagement> section from a Maven pom.xml file. This method parses the XML structure of the POM file, locates the <dependencies> section within <dependencyManagement>, and retrieves all dependency defined within it.

        Example usage:
        - Input: pom.xml file containing dependencies like <dependencyManagement><dependencies><dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version><scope>provided</scope></dependency></dependencies></dependencyManagement>
        - Output: Collection containing: {"groupId": "jakarta.platform", "artifactId": "jakarta.jakartaee-api", "version": "${version.jakarta.ee}", "scope": "provided"}

        The method handles XML parsing automatically and returns an empty collection if no <dependencies> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(title = "gets all the dependency management dependencies", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllDependenciesManagement() throws IOException, XmlPullParserException {
        log.info("gets all the dependencies in the dependencyManagement section");

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getDependencyManagement() == null) {
            return ToolResponse.success("No dependencyManagement section in the pom.xml file.");
        }
        if (model.getDependencyManagement().getDependencies().isEmpty()) {
            return ToolResponse.success("No dependencies in the dependencyManagement in the pom.xml file.");
        }

        // Builds the list of dependencies
        List<DependencyRecord> dependencies = model.getDependencyManagement().getDependencies().stream()
            .map(dependency -> new DependencyRecord(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getType(),
                dependency.getScope()))
            .collect(Collectors.toList());

        return ToolResponse.success(toJson(dependencies));
    }

    @Tool(name = "gets_all_the_properties", description = """
        This method returns all existing properties from a Maven pom.xml file. This method parses the XML structure of the POM file, locates the <properties> section, and retrieves all property name-value pairs defined within it.

        Example usage:
        - Input: pom.xml file containing properties like <java.version>11</java.version>, <maven.compiler.source>11</maven.compiler.source>
        - Output: Collection containing key-value pairs: {"java.version": "11", "maven.compiler.source": "11"}

        The method handles XML parsing automatically and returns an empty collection if no <properties> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(title = "gets all the properties", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllProperties() throws IOException, XmlPullParserException {
        log.info("gets all the properties");

        // Read the pom.xml file
        Model model = readModel();

        // Performs checks
        if (model.getProperties().isEmpty()) {
            return ToolResponse.success("No properties found in the pom.xml file.");
        }

        // Builds the list of properties
        List<PropertyRecord> properties = model.getProperties().entrySet().stream()
            .map(entry -> new PropertyRecord((String) entry.getKey(), (String) entry.getValue()))
            .collect(Collectors.toList());

        return ToolResponse.success(toJson(properties));
    }

    @Tool(name = "adds_a_new_property", description = """
        This method adds a new property to an existing Maven pom.xml file. This method takes a property and inserts them into the <properties> section of the POM file. If no <properties> section exists, the method creates one. The method preserves the existing XML structure and formatting while safely adding the new property without overwriting existing properties or corrupting the file structure.

        Example usage:
        - Input: property name "java.version", value "11"
        - Result: Adds <java.version>11</java.version> to the <properties> section of the pom.xml

        The method handles XML parsing, property insertion, and file writing operations automatically.
        """,
        annotations = @Annotations(title = "adds a new property", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
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
        annotations = @Annotations(title = "updates the value of an existing property", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
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
        annotations = @Annotations(title = "removes an existing property", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
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

    public static String toJson(Object object) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(object);
    }
}

record PropertyRecord(String key, String value) {
}

record DependencyRecord(String groupId, String artifactId, String version, String type, String scope) {
}

record PluginRecord(String groupId, String artifactId, String version, String inherited,
                    List<DependencyRecord> dependencies) {
}

record ProfileRecord(String id) {
}
