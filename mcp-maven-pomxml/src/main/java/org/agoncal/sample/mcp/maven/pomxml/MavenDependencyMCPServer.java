package org.agoncal.sample.mcp.maven.pomxml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.Tool.Annotations;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

public class MavenDependencyMCPServer {

    private static final Logger log = Logger.getLogger(MavenDependencyMCPServer.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Inject
    MavenDependencyService mavenService;

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

        List<ProfileRecord> profiles = mavenService.getAllProfiles();

        if (profiles.isEmpty()) {
            return ToolResponse.success("No profiles in the pom.xml file.");
        }

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

        List<PluginRecord> plugins = mavenService.getAllPlugins();

        if (plugins.isEmpty()) {
            return ToolResponse.success("No plugins in the pom.xml file.");
        }

        return ToolResponse.success(toJson(plugins));
    }

    @Tool(name = "removes_an_existing_plugin", description = """
        This method deletes an existing plugin from a Maven pom.xml file. This method takes a plugin groupId and artifactId as a parameter, locates the specified plugin within the <plugins> section of the POM file, and removes it entirely while preserving all other plugins and the XML structure. The method only removes existing plugins and does not modify the file if the specified key is not found. If profileId is null, the plugin will be removed from the main POM; otherwise, it will be removed from the specified profile.

        Example usage:
        - Input: profileId null, groupId "org.hibernate.orm", artifactId "hibernate-core"
        - Before: <plugins><plugin><groupId>org.hibernate.orm</groupId><artifactId>hibernate-core</artifactId><version>6.0.9.Final</version></plugin><plugin><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></plugin></plugins>
        - After: <plugins><plugin><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version></plugin></plugins>

        The method handles XML parsing, plugin location, element removal, and file writing operations automatically. If the specified plugin does not exist in the pom.xml, the method typically returns an error or indication that the plugin was not found, without modifying the file. If removing the plugin results in an empty <plugins> section, the implementation may choose to either keep the empty section or remove it entirely.
        """,
        annotations = @Annotations(title = "removes an existing plugin", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingPlugin(
        @ToolArg(name = "profile id", description = "The profile ID to remove the plugin from (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the plugin to be removed.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the plugin to be removed.") String artifactId)
        throws IOException, XmlPullParserException {
        log.info("remove the existing plugin " + groupId + " " + artifactId +
            (profileId != null ? " from profile: " + profileId : " from main POM"));

        try {
            mavenService.removePlugin(profileId, groupId, artifactId);
            return ToolResponse.success("The existing plugin '" + groupId + ":" + artifactId + "' has been removed" +
                (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "gets_all_the_dependencies", description = """
        This method returns all existing dependencies from a Maven pom.xml file. This means the dependencies in the main project but also all the dependencies for all profiles. This method parses the XML structure of the POM file, locates the <dependencies> sections in the main and in the profiles, and retrieves all dependency defined within it.

        Example usage:
        - Input: pom.xml file containing dependencies like <dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version><scope>provided</scope></dependency>    <profile><id>jakarta-ee</id><dependencies><dependency><groupId>org.apache.derby</groupId><artifactId>derby</artifactId><version>${version.derby}</version><scope>test</scope></dependency></dependencies></profile>

        - Output: Collection containing: {"groupId": "jakarta.platform", "artifactId": "jakarta.jakartaee-api", "version": "${version.jakarta.ee}", "scope": "provided", "profile": "jakarta-ee", "groupId": :"org.apache.derby", "artifactId": "derby", "version": "${version.derby}", "scope": "test"}

        The method handles XML parsing automatically and returns an empty collection if no <dependencies> section exists in the pom.xml file. It reads the file without modifying it.
        """,
        annotations = @Annotations(title = "gets all the dependencies", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllDependencies() throws IOException, XmlPullParserException {
        log.info("gets all the dependencies");

        List<DependencyRecord> dependencies = mavenService.getAllDependencies();

        if (dependencies.isEmpty()) {
            return ToolResponse.success("No dependencies in the pom.xml file.");
        }

        return ToolResponse.success(toJson(dependencies));
    }

    @Tool(name = "adds_a_new_dependency", description = """
        This method adds a new dependency to an existing Maven pom.xml file. This method takes a dependency and inserts them into the <dependencies> section of the POM file. If no <dependencies> section exists, the method creates one. The method preserves the existing XML structure and formatting while safely adding the new dependency without overwriting existing dependencies or corrupting the file structure. If profileId is null, the dependency will be added to the main POM; otherwise, it will be added to the specified profile.

        Example usage:
        - Input: profileId null, dependency name "jakarta.platform", "jakarta.jakartaee-api", "${version.jakarta.ee}", "provided"
        - Result: Adds <dependency><groupId>jakarta.platform</groupId><artifactId>jakarta.jakartaee-api</artifactId><version>${version.jakarta.ee}</version><scope>provided</scope></dependency> to the <dependencies> section of the pom.xml

        The method handles XML parsing, dependency insertion, and file writing operations automatically.
        """,
        annotations = @Annotations(title = "adds a new dependency", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse addNewDependency(
        @ToolArg(name = "profile id", description = "The profile ID to add the dependency to (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the dependency key to be added.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency key to be added.") String artifactId,
        @ToolArg(name = "version", description = "The version of the dependency key to be added.") String version,
        @ToolArg(name = "type", description = "The type of the dependency key to be added. Can be jar, pom. The default is jar so you don't need to add <type>jar</type>") String type,
        @ToolArg(name = "scope", description = "The scope of the dependency key to be added. Can be compile, provided, runtime, test, system, import. The default is compile so you don't need to add <scope>compile</scope>") String scope)
        throws IOException, XmlPullParserException {
        log.info("adds the new dependency " + groupId + " " + artifactId + " " + version + " " + type + " " + scope +
            (profileId != null ? " to profile: " + profileId : " to main POM"));

        try {
            mavenService.addDependency(profileId, groupId, artifactId, version, type, scope);
            return ToolResponse.success("The new dependency " + groupId + ":" + artifactId + ":" + version + " has been added" +
                (profileId != null ? " to profile '" + profileId + "'" : " to main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
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

        try {
            mavenService.updateDependencyVersion(groupId, artifactId, version);
            return ToolResponse.success("The version of the existing dependency " + groupId + ":" + artifactId + " has been updated to " + version);
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
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

        try {
            mavenService.removeDependency(groupId, artifactId);
            return ToolResponse.success("The existing dependency '" + groupId + ":" + artifactId + "' has been removed");
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
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

        List<DependencyRecord> dependencies = mavenService.getAllDependencyManagementDependencies();

        if (dependencies.isEmpty()) {
            return ToolResponse.success("No dependencies in the dependencyManagement in the pom.xml file.");
        }

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

        List<PropertyRecord> properties = mavenService.getAllProperties();

        if (properties.isEmpty()) {
            return ToolResponse.success("No properties found in the pom.xml file.");
        }

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

        try {
            mavenService.addProperty(key, value);
            return ToolResponse.success("The new property " + key + " has been added with value " + value);
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
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

        try {
            mavenService.updatePropertyValue(key, value);
            return ToolResponse.success("The value of the existing property " + key + " has been updated to " + value);
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
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

        try {
            mavenService.removeProperty(key);
            return ToolResponse.success("The existing property " + key + " has been removed");
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    public static String toJson(Object object) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(object);
    }
}
