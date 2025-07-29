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
import org.agoncal.sample.mcp.maven.pomxml.model.DependencyRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.ParentRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.PluginRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.ProfileRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.PropertyRecord;
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
        Retrieves all Maven profiles from the pom.xml file.

        **Purpose**: Get a list of all profile IDs defined in the Maven POM file
        **Input**: None (reads from configured POM file)
        **Output**: JSON array of profile objects with their IDs
        **Side effects**: None (read-only operation)

        **When to use**:
        - When you need to see what profiles are available in the project
        - Before working with profile-specific dependencies, properties, or plugins
        - To understand project build variants

        **Example output**:
        ```json
        [
          {"id": "jakarta-ee"},
          {"id": "jacoco"},
          {"id": "production"}
        ]
        ```

        Returns empty message if no profiles exist in the POM file.
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
        Retrieves all Maven plugins from the pom.xml file.

        **Purpose**: Get a list of all plugins defined in the Maven POM file (from main POM and all profiles)
        **Input**: None (reads from configured POM file)
        **Output**: JSON array of plugin objects with their details
        **Side effects**: None (read-only operation)

        **When to use**:
        - When you need to see what plugins are configured in the project
        - To check plugin versions before updating them
        - To understand the build configuration and tooling

        **Example output**:
        ```json
        [
          {
            "groupId": "org.apache.maven.plugins",
            "artifactId": "maven-compiler-plugin",
            "version": "${version.maven.compiler.plugin}",
            "inherited": true,
            "profile": null
          },
          {
            "groupId": "org.jacoco",
            "artifactId": "jacoco-maven-plugin",
            "version": "0.8.10",
            "inherited": false,
            "profile": "jacoco"
          }
        ]
        ```

        Returns empty message if no plugins exist in the POM file.
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

    @Tool(name = "adds_a_new_plugin", description = """
        Adds a new Maven plugin to the pom.xml file.

        **Purpose**: Add a new plugin to the Maven POM file or a specific profile
        **Input**: Profile ID, groupId, artifactId, version, and inheritance setting
        **Output**: Success message confirming addition or error if plugin already exists
        **Side effects**: Modifies the POM file by adding the specified plugin

        **When to use**:
        - When you need to add a new build plugin to the project
        - When adding profile-specific build tools
        - When configuring new Maven functionality

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID (e.g., "jacoco", "production")
        - `group id`: Maven groupId of the plugin (e.g., "org.apache.maven.plugins")
        - `artifact id`: Maven artifactId of the plugin (e.g., "maven-compiler-plugin")
        - `version`: Version string (e.g., "3.8.1", "${version.maven.compiler.plugin}")
        - `inherited`: Whether the plugin is inherited by child modules (true/false) - defaults to true if null

        **Example**: Adding a new Maven plugin for code coverage.

        **Error conditions**: Returns error if plugin already exists in specified location.
        """,
        annotations = @Annotations(title = "adds a new plugin", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse addNewPlugin(
        @ToolArg(name = "profile id", description = "The profile ID to add the plugin to (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the plugin to be added.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the plugin to be added.") String artifactId,
        @ToolArg(name = "version", description = "The version of the plugin to be added.") String version,
        @ToolArg(name = "inherited", description = "Whether the plugin is inherited by child modules (true/false). Defaults to true if null.") Boolean inherited)
        throws IOException, XmlPullParserException {
        log.info("adds the new plugin " + groupId + " " + artifactId + " " + version + " " + inherited +
            (profileId != null ? " to profile: " + profileId : " to main POM"));

        try {
            mavenService.addNewPlugin(profileId, groupId, artifactId, version, inherited);
            return ToolResponse.success("The new plugin '" + groupId + ":" + artifactId + ":" + version + "' has been added" +
                (profileId != null ? " to profile '" + profileId + "'" : " to main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "removes_an_existing_plugin", description = """
        Removes an existing Maven plugin from the pom.xml file.

        **Purpose**: Delete a specific plugin from the Maven POM file or a specific profile
        **Input**: Profile ID (null for main POM), groupId and artifactId of the plugin to remove
        **Output**: Success message confirming removal or error if plugin not found
        **Side effects**: Modifies the POM file by removing the specified plugin

        **When to use**:
        - When you need to remove unused or unwanted plugins from the build configuration
        - When cleaning up obsolete build tools
        - When removing profile-specific plugins

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID (e.g., "jacoco", "production")
        - `group id`: Maven groupId of the plugin (e.g., "org.apache.maven.plugins")
        - `artifact id`: Maven artifactId of the plugin (e.g., "maven-compiler-plugin")

        **Example**: Removing jacoco plugin from "jacoco" profile:
        - Before: Plugin exists in profile
        - After: Plugin completely removed from that profile

        **Error conditions**: Returns error if plugin doesn't exist in specified location.
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
            mavenService.removeExistingPlugin(profileId, groupId, artifactId);
            return ToolResponse.success("The existing plugin '" + groupId + ":" + artifactId + "' has been removed" +
                (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "gets_all_the_dependencies", description = """
        Retrieves all Maven dependencies from the pom.xml file.

        **Purpose**: Get a comprehensive list of all dependencies from the main POM and all profiles
        **Input**: None (reads from configured POM file)
        **Output**: JSON array of dependency objects with their details
        **Side effects**: None (read-only operation)

        **When to use**:
        - When you need to see all project dependencies across all profiles
        - To audit the project's dependency tree
        - Before adding new dependencies to avoid duplicates
        - To understand what libraries the project uses

        **Example output**:
        ```json
        [
          {
            "groupId": "jakarta.platform",
            "artifactId": "jakarta.jakartaee-api",
            "version": "${version.jakarta.ee}",
            "scope": "provided",
            "type": "jar",
            "profile": null
          },
          {
            "groupId": "org.apache.derby",
            "artifactId": "derby",
            "version": "${version.derby}",
            "scope": "test",
            "type": "jar",
            "profile": "jakarta-ee"
          }
        ]
        ```

        Returns empty message if no dependencies exist in the POM file.
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
        Adds a new Maven dependency to the pom.xml file.

        **Purpose**: Add a new dependency to the Maven POM file or a specific profile
        **Input**: Profile ID, groupId, artifactId, version, type, and scope of the dependency
        **Output**: Success message confirming addition or error if dependency already exists
        **Side effects**: Modifies the POM file by adding the specified dependency

        **When to use**:
        - When you need to add a new library or framework to the project
        - When adding test-specific dependencies
        - When adding profile-specific dependencies

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID (e.g., "jakarta-ee", "test")
        - `group id`: Maven groupId (e.g., "org.junit.jupiter")
        - `artifact id`: Maven artifactId (e.g., "junit-jupiter")
        - `version`: Version string (e.g., "5.8.2", "${version.junit}")
        - `type`: Dependency type (jar, pom, etc.) - defaults to "jar" if null
        - `scope`: Dependency scope (compile, test, provided, runtime) - defaults to "compile" if null

        **Example**: Adding JUnit dependency to test scope in main POM.

        **Error conditions**: Returns error if dependency already exists in specified location.
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
            mavenService.addNewDependency(profileId, groupId, artifactId, version, type, scope);
            return ToolResponse.success("The new dependency " + groupId + ":" + artifactId + ":" + version + " has been added" +
                (profileId != null ? " to profile '" + profileId + "'" : " to main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "updates_the_version_of_an_existing_dependency", description = """
        Updates the version of an existing Maven dependency in the pom.xml file.

        **Purpose**: Change the version of an existing dependency in the Maven POM or profile
        **Input**: Profile ID, groupId, artifactId, and new version
        **Output**: Success message confirming update or error if dependency not found
        **Side effects**: Modifies the POM file by updating the dependency version

        **When to use**:
        - When upgrading or downgrading library versions
        - When fixing security vulnerabilities by updating versions
        - When standardizing versions across profiles

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `group id`: Maven groupId of existing dependency
        - `artifact id`: Maven artifactId of existing dependency
        - `version`: New version string (e.g., "5.9.0", "${version.junit}")

        **Example**: Updating JUnit from version 5.8.2 to 5.9.0.

        **Error conditions**: Returns error if dependency doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "updates the version of an existing dependency", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingDependencyVersion(
        @ToolArg(name = "profile id", description = "The profile ID to update the dependency in (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the dependency key to be updated.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency key to be updated.") String artifactId,
        @ToolArg(name = "version", description = "The new version of the dependency to be updated.") String version)
        throws IOException, XmlPullParserException {
        log.info("updates the existing dependency " + groupId + " " + artifactId + " " + version + " in profile " + profileId);

        try {
            mavenService.updateDependencyVersion(profileId, groupId, artifactId, version);
            return ToolResponse.success("The version of the existing dependency " + groupId + ":" + artifactId + " has been updated to " + version + (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "removes_an_existing_dependency", description = """
        Removes an existing Maven dependency from the pom.xml file.

        **Purpose**: Delete a specific dependency from the Maven POM file or a specific profile
        **Input**: Profile ID, groupId and artifactId of the dependency to remove
        **Output**: Success message confirming removal or error if dependency not found
        **Side effects**: Modifies the POM file by removing the specified dependency

        **When to use**:
        - When removing unused or deprecated dependencies
        - When cleaning up the project's dependency tree
        - When removing profile-specific dependencies

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `group id`: Maven groupId of the dependency to remove
        - `artifact id`: Maven artifactId of the dependency to remove

        **Example**: Removing an unused test library from the project.

        **Error conditions**: Returns error if dependency doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "removes an existing dependency", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingDependency(
        @ToolArg(name = "profile id", description = "The profile ID to remove the dependency from (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the dependency to be removed.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency to be removed.") String artifactId)
        throws IOException, XmlPullParserException {
        log.info("remove the existing dependency " + groupId + " " + artifactId + " from profile " + profileId);

        try {
            mavenService.removeExistingDependency(profileId, groupId, artifactId);
            return ToolResponse.success("The existing dependency '" + groupId + ":" + artifactId + "' has been removed" + (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "gets_all_the_dependencies_in_dependency_managements", description = """
        Retrieves all Maven dependency management entries from the pom.xml file.

        **Purpose**: Get all dependencies defined in the dependencyManagement section for version control
        **Input**: None (reads from configured POM file)
        **Output**: JSON array of dependency management objects
        **Side effects**: None (read-only operation)

        **When to use**:
        - When you need to see what dependency versions are centrally managed
        - To understand version inheritance for child modules
        - Before adding new dependency management entries
        - To audit centralized dependency version control

        **Example output**:
        ```json
        [
          {
            "groupId": "org.jboss.arquillian",
            "artifactId": "arquillian-bom",
            "version": "${version.arquillian}",
            "type": "pom",
            "scope": "import",
            "profile": null
          }
        ]
        ```

        Returns empty message if no dependency management exists in the POM file.
        """,
        annotations = @Annotations(title = "gets all the dependency management dependencies", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllDependenciesInDependencyManagements() throws IOException, XmlPullParserException {
        log.info("gets all the dependencies in the dependencyManagement section");

        List<DependencyRecord> dependencies = mavenService.getAllDependenciesInDependencyManagements();

        if (dependencies.isEmpty()) {
            return ToolResponse.success("No dependencies in the dependencyManagement in the pom.xml file.");
        }

        return ToolResponse.success(toJson(dependencies));
    }

    @Tool(name = "gets_all_the_dependency_managements", description = """
        Retrieves all Maven dependency management entries from the pom.xml file.

        **Purpose**: Get all dependencies defined in the dependencyManagement section for version control
        **Input**: None (reads from configured POM file)
        **Output**: JSON array of dependency management objects
        **Side effects**: None (read-only operation)

        **When to use**:
        - When you need to see what dependency versions are centrally managed
        - To understand version inheritance for child modules
        - Before adding new dependency management entries
        - To audit centralized dependency version control

        **Example output**:
        ```json
        [
          {
            "groupId": "org.jboss.arquillian",
            "artifactId": "arquillian-bom",
            "version": "${version.arquillian}",
            "type": "pom",
            "scope": "import",
            "profile": null
          }
        ]
        ```

        Returns empty message if no dependency management exists in the POM file.
        """,
        annotations = @Annotations(title = "gets all the dependency managements", readOnlyHint = true, destructiveHint = false, idempotentHint = false))
    public ToolResponse getAllDependencyManagements() throws IOException, XmlPullParserException {
        log.info("gets all the dependency managements");

        List<DependencyRecord> dependencies = mavenService.getAllDependencyManagements();

        if (dependencies.isEmpty()) {
            return ToolResponse.success("No dependencies in the dependencyManagement in the pom.xml file.");
        }

        return ToolResponse.success(toJson(dependencies));
    }

    @Tool(name = "adds_a_new_dependency_in_dependency_management", description = """
        Adds a new Maven dependency to the dependencyManagement section of the pom.xml file.

        **Purpose**: Add a new dependency to the dependencyManagement section for version control
        **Input**: Profile ID, groupId, artifactId, version, type, and scope of the dependency
        **Output**: Success message confirming addition or error if dependency already exists
        **Side effects**: Modifies the POM file by adding the specified dependency to dependencyManagement

        **When to use**:
        - When you need to centrally manage dependency versions
        - When setting up Bill of Materials (BOM) dependencies
        - When defining versions for child modules to inherit
        - When managing version consistency across profiles

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID (e.g., "jakarta-ee", "production")
        - `group id`: Maven groupId (e.g., "org.jboss.arquillian")
        - `artifact id`: Maven artifactId (e.g., "arquillian-bom")
        - `version`: Version string (e.g., "1.7.0.Alpha10", "${version.arquillian}")
        - `type`: Dependency type (jar, pom, etc.) - defaults to "jar" if null
        - `scope`: Dependency scope (compile, test, provided, runtime, import) - defaults to "compile" if null

        **Example**: Adding a BOM dependency for version management.

        **Error conditions**: Returns error if dependency already exists in specified location.
        """,
        annotations = @Annotations(title = "adds a new dependency in dependency management", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse addNewDependencyInDependencyManagement(
        @ToolArg(name = "profile id", description = "The profile ID to add the dependency to (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the dependency to be added.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency to be added.") String artifactId,
        @ToolArg(name = "version", description = "The version of the dependency to be added.") String version,
        @ToolArg(name = "type", description = "The type of the dependency to be added. Can be jar, pom. The default is jar so you don't need to add <type>jar</type>") String type,
        @ToolArg(name = "scope", description = "The scope of the dependency to be added. Can be compile, provided, runtime, test, system, import. The default is compile so you don't need to add <scope>compile</scope>") String scope)
        throws IOException, XmlPullParserException {
        log.info("adds the new dependencyManagement dependency " + groupId + " " + artifactId + " " + version + " " + type + " " + scope +
            (profileId != null ? " to profile: " + profileId : " to main POM"));

        try {
            mavenService.addNewDependencyInDependencyManagement(profileId, groupId, artifactId, version, type, scope);
            return ToolResponse.success("The new dependencyManagement dependency " + groupId + ":" + artifactId + ":" + version + " has been added" +
                (profileId != null ? " to profile '" + profileId + "'" : " to main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "gets_all_the_properties", description = """
        Retrieves all Maven properties from the pom.xml file.

        **Purpose**: Get all property key-value pairs from the main POM and all profiles
        **Input**: None (reads from configured POM file)
        **Output**: JSON array of property objects with their keys, values, and profile context
        **Side effects**: None (read-only operation)

        **When to use**:
        - When you need to see what properties are defined in the project
        - To understand variable substitution in the POM
        - Before adding new properties to avoid duplicates
        - To audit property usage across profiles

        **Example output**:
        ```json
        [
          {
            "key": "version.java",
            "value": "17",
            "profile": null
          },
          {
            "key": "version.jakarta.ee",
            "value": "10.1.2",
            "profile": "jakarta-ee"
          }
        ]
        ```

        Returns empty message if no properties exist in the POM file.
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
        Adds a new Maven property to the pom.xml file.

        **Purpose**: Add a new property key-value pair to the Maven POM or a specific profile
        **Input**: Profile ID, property key, and property value
        **Output**: Success message confirming addition or error if property already exists
        **Side effects**: Modifies the POM file by adding the specified property

        **When to use**:
        - When defining new version variables for dependencies
        - When adding configuration properties for plugins
        - When setting profile-specific properties
        - When centralizing commonly used values

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `property key`: Property name (e.g., "version.junit", "maven.compiler.source")
        - `property value`: Property value (e.g., "5.9.0", "17")

        **Example**: Adding a new version property for a library.

        **Error conditions**: Returns error if property already exists in specified location.
        """,
        annotations = @Annotations(title = "adds a new property", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse addNewProperty(
        @ToolArg(name = "profile id", description = "The profile ID to add the property to (null for main POM).") String profileId,
        @ToolArg(name = "property key", description = "The name of the property key to be added.") String key,
        @ToolArg(name = "property value", description = "The value of property to be added.") String value)
        throws IOException, XmlPullParserException {
        log.info("adds the new property " + key + " with value " + value + " to profile " + profileId);

        try {
            mavenService.addNewProperty(profileId, key, value);
            return ToolResponse.success("The new property " + key + " has been added with value " + value + (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "updates_the_value_of_an_existing_property", description = """
        Updates the value of an existing Maven property in the pom.xml file.

        **Purpose**: Change the value of an existing property in the Maven POM or a specific profile
        **Input**: Profile ID, property key, and new property value
        **Output**: Success message confirming update or error if property not found
        **Side effects**: Modifies the POM file by updating the specified property value

        **When to use**:
        - When updating version numbers for dependencies or plugins
        - When changing configuration values
        - When updating profile-specific property values
        - When standardizing property values across profiles

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `property key`: Existing property name to update
        - `property value`: New value for the property

        **Example**: Updating Java version from 11 to 17.

        **Error conditions**: Returns error if property doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "updates the value of an existing property", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingPropertyValue(
        @ToolArg(name = "profile id", description = "The profile ID to update the property in (null for main POM).") String profileId,
        @ToolArg(name = "property key", description = "The name of the property key to look for.") String key,
        @ToolArg(name = "property value", description = "The new value of the existing property.") String value)
        throws IOException, XmlPullParserException {
        log.info("updates the existing property " + key + " with the new value " + value + " in profile " + profileId);

        try {
            mavenService.updatePropertyValue(profileId, key, value);
            return ToolResponse.success("The value of the existing property " + key + " has been updated to " + value + (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "removes_an_existing_property", description = """
        Removes an existing Maven property from the pom.xml file.

        **Purpose**: Delete a specific property from the Maven POM file or a specific profile
        **Input**: Profile ID and property key to remove
        **Output**: Success message confirming removal or error if property not found
        **Side effects**: Modifies the POM file by removing the specified property

        **When to use**:
        - When cleaning up unused properties
        - When removing deprecated configuration values
        - When removing profile-specific properties
        - When consolidating property definitions

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `property key`: Property name to remove

        **Example**: Removing an obsolete version property.

        **Error conditions**: Returns error if property doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "removes an existing property", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingProperty(
        @ToolArg(name = "profile id", description = "The profile ID to remove the property from (null for main POM).") String profileId,
        @ToolArg(name = "property key", description = "The name of the property key to remove.") String key)
        throws IOException, XmlPullParserException {
        log.info("remove the existing property " + key + " from profile " + profileId);

        try {
            mavenService.removeExistingProperty(profileId, key);
            return ToolResponse.success("The existing property " + key + " has been removed" + (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "removes_an_existing_dependency_management_dependency", description = """
        Removes an existing Maven dependency from the dependencyManagement section of the pom.xml file.

        **Purpose**: Delete a specific dependency from the dependencyManagement section
        **Input**: Profile ID, groupId and artifactId of the dependency to remove
        **Output**: Success message confirming removal or error if dependency not found
        **Side effects**: Modifies the POM file by removing the specified dependency from dependencyManagement

        **When to use**:
        - When cleaning up unused dependency management entries
        - When removing deprecated BOM dependencies
        - When consolidating dependency management across profiles
        - When removing version control that's no longer needed

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `group id`: Maven groupId of the dependency to remove
        - `artifact id`: Maven artifactId of the dependency to remove

        **Example**: Removing an obsolete BOM dependency from dependencyManagement.

        **Error conditions**: Returns error if dependency doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "removes an existing dependency management dependency", readOnlyHint = false, destructiveHint = true, idempotentHint = false))
    public ToolResponse removeExistingDependencyInDependencyManagement(
        @ToolArg(name = "profile id", description = "The profile ID to remove the dependency from (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the dependency to be removed.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency to be removed.") String artifactId)
        throws IOException, XmlPullParserException {
        log.info("remove the existing dependencyManagement dependency " + groupId + " " + artifactId +
            (profileId != null ? " from profile: " + profileId : " from main POM"));

        try {
            mavenService.removeExistingDependencyInDependencyManagement(profileId, groupId, artifactId);
            return ToolResponse.success("The existing dependencyManagement dependency '" + groupId + ":" + artifactId + "' has been removed" +
                (profileId != null ? " from profile '" + profileId + "'" : " from main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "updates_an_existing_plugin_version", description = """
        Updates the version of an existing Maven plugin in the pom.xml file.

        **Purpose**: Change the version of an existing plugin in the Maven POM or profile
        **Input**: Profile ID, groupId, artifactId, and new version
        **Output**: Success message confirming update or error if plugin not found
        **Side effects**: Modifies the POM file by updating the plugin version

        **When to use**:
        - When upgrading or downgrading plugin versions
        - When fixing security vulnerabilities by updating plugin versions
        - When standardizing plugin versions across profiles
        - When updating build tool versions

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `group id`: Maven groupId of existing plugin
        - `artifact id`: Maven artifactId of existing plugin
        - `version`: New version string (e.g., "3.8.1", "${version.maven.compiler.plugin}")

        **Example**: Updating Maven Compiler Plugin from version 3.8.1 to 3.9.0.

        **Error conditions**: Returns error if plugin doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "updates an existing plugin version", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingPluginVersion(
        @ToolArg(name = "profile id", description = "The profile ID to update the plugin in (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the plugin to be updated.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the plugin to be updated.") String artifactId,
        @ToolArg(name = "version", description = "The new version of the plugin to be updated.") String version)
        throws IOException, XmlPullParserException {
        log.info("updates the existing plugin " + groupId + " " + artifactId + " " + version +
            (profileId != null ? " in profile: " + profileId : " in main POM"));

        try {
            mavenService.updatePluginVersion(profileId, groupId, artifactId, version);
            return ToolResponse.success("The version of the existing plugin " + groupId + ":" + artifactId + " has been updated to " + version +
                (profileId != null ? " in profile '" + profileId + "'" : " in main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "updates_an_existing_dependency_management_dependency_version", description = """
        Updates the version of an existing Maven dependency in the dependencyManagement section of the pom.xml file.

        **Purpose**: Change the version of an existing dependency in the dependencyManagement section
        **Input**: Profile ID, groupId, artifactId, and new version
        **Output**: Success message confirming update or error if dependency not found
        **Side effects**: Modifies the POM file by updating the dependency version in dependencyManagement

        **When to use**:
        - When upgrading or downgrading managed dependency versions
        - When fixing security vulnerabilities by updating BOM versions
        - When standardizing dependency versions across profiles
        - When updating centrally managed library versions

        **Parameters**:
        - `profile id`: null for main POM, or specific profile ID
        - `group id`: Maven groupId of existing dependency in dependencyManagement
        - `artifact id`: Maven artifactId of existing dependency in dependencyManagement
        - `version`: New version string (e.g., "1.8.0", "${version.arquillian}")

        **Example**: Updating Arquillian BOM version in dependencyManagement.

        **Error conditions**: Returns error if dependency doesn't exist in specified location.
        """,
        annotations = @Annotations(title = "updates an existing dependency management dependency version", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateExistingDependencyManagementDependencyVersion(
        @ToolArg(name = "profile id", description = "The profile ID to update the dependency in (null for main POM).") String profileId,
        @ToolArg(name = "group id", description = "The group id of the dependency to be updated.") String groupId,
        @ToolArg(name = "artifact id", description = "The artifact id of the dependency to be updated.") String artifactId,
        @ToolArg(name = "version", description = "The new version of the dependency to be updated.") String version)
        throws IOException, XmlPullParserException {
        log.info("updates the existing dependencyManagement dependency " + groupId + " " + artifactId + " " + version +
            (profileId != null ? " in profile: " + profileId : " in main POM"));

        try {
            mavenService.updateDependencyManagementDependencyVersion(profileId, groupId, artifactId, version);
            return ToolResponse.success("The version of the existing dependencyManagement dependency " + groupId + ":" + artifactId + " has been updated to " + version +
                (profileId != null ? " in profile '" + profileId + "'" : " in main POM"));
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "gets_parent", description = """
        Gets the parent POM information from the Maven pom.xml file.

        **Purpose**: Retrieve information about the parent POM that this project inherits from
        **Input**: None (reads from current POM)
        **Output**: JSON containing parent groupId, artifactId, version, and relativePath, or null if no parent
        **Side effects**: Read-only operation, no modifications to POM

        **When to use**:
        - When checking what parent POM the project inherits from
        - When analyzing project inheritance structure
        - When verifying parent versions before updates
        - When documenting project dependencies

        **Parameters**: None required

        **Output format**:
        ```json
        {
          "groupId": "org.springframework.boot",
          "artifactId": "spring-boot-starter-parent", 
          "version": "3.5.4",
          "relativePath": "../"
        }
        ```

        **Error conditions**: Returns null JSON value if no parent exists in the POM.
        """,
        annotations = @Annotations(title = "gets parent", readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public ToolResponse getParent() throws IOException, XmlPullParserException {
        log.info("gets parent information");

        try {
            ParentRecord parent = mavenService.getParent();
            if (parent == null) {
                return ToolResponse.success("null");
            }
            return ToolResponse.success(toJson(parent));
        } catch (Exception e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(name = "updates_parent_version", description = """
        Updates the version of the parent POM in the Maven pom.xml file.

        **Purpose**: Change the version of the parent POM that this project inherits from
        **Input**: New version for the parent
        **Output**: Success message confirming update or error if no parent exists
        **Side effects**: Modifies the POM file by updating the parent version

        **When to use**:
        - When upgrading to a newer version of a parent POM (e.g., Spring Boot starter parent)
        - When updating corporate parent POM versions
        - When migrating to different framework versions
        - When following parent POM security updates

        **Parameters**:
        - `version`: New version string for the parent (e.g., "3.2.1", "2.7.18")

        **Example**: Updating Spring Boot parent from 3.5.4 to 3.2.1.

        **Error conditions**: Returns error if no parent exists in the POM.
        """,
        annotations = @Annotations(title = "updates parent version", readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public ToolResponse updateParentVersion(
        @ToolArg(name = "version", description = "The new version for the parent POM.") String version)
        throws IOException, XmlPullParserException {
        log.info("updates parent version to " + version);

        try {
            mavenService.updateParentVersion(version);
            return ToolResponse.success("The parent version has been updated to " + version);
        } catch (IllegalArgumentException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    private static String toJson(Object object) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(object);
    }
}
