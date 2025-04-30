package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.annotation.PostConstruct;
import org.jboss.logging.Logger;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.AddManagedDependency;
import org.openrewrite.maven.AddParentPom;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.AddPluginDependency;
import org.openrewrite.maven.AddProperty;
import org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId;
import org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId;
import org.openrewrite.maven.ChangeParentPom;
import org.openrewrite.maven.ChangePluginConfiguration;
import org.openrewrite.maven.ChangePluginGroupIdAndArtifactId;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.OrderPomElements;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.maven.RemoveDuplicateDependencies;
import org.openrewrite.maven.RemoveManagedDependency;
import org.openrewrite.maven.RemovePlugin;
import org.openrewrite.maven.RemovePluginDependency;
import org.openrewrite.maven.RemoveProperty;
import org.openrewrite.maven.UpdateMavenProjectPropertyJavaVersion;
import org.openrewrite.maven.UpdateMavenWrapper;
import org.openrewrite.maven.UpgradeDependencyVersion;
import org.openrewrite.maven.UpgradeParentVersion;
import org.openrewrite.maven.UpgradePluginVersion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MavenMigrationMCPServer {

    private static final Logger log = Logger.getLogger(MavenMigrationMCPServer.class);
    private static final String ROOT_APP_TO_MIGRATE = System.getenv("ROOT_APP_TO_MIGRATE");
    private static final Path ROOT_PATH = Paths.get(ROOT_APP_TO_MIGRATE);
    private static final File ROOT_DIRECTORY = Paths.get(ROOT_APP_TO_MIGRATE).toFile();
    private static ExecutionContext executionContext;
    private static List<SourceFile> sourceFiles;

    @PostConstruct
    void findPomXMLFiles() {
        log.info("Finding the number of pom.xml files in the directory: " + ROOT_APP_TO_MIGRATE);
        List<Path> pomXmlFiles = new ArrayList<>();
        if (ROOT_DIRECTORY.exists()) {
            collectPomXMLFiles(ROOT_DIRECTORY, pomXmlFiles);
        } else {
            System.err.println("Directory does not exist: " + ROOT_DIRECTORY);
        }
        log.info("Found " + pomXmlFiles.size() + " pom.xml files in the directory: " + ROOT_DIRECTORY);

        // Create execution context
        executionContext = new InMemoryExecutionContext(t -> t.printStackTrace());

        // Create Maven parser
        MavenParser mavenParser = MavenParser.builder().build();

        // Parse the POM XML files
        sourceFiles = mavenParser.parse(pomXmlFiles, ROOT_PATH, executionContext).collect(Collectors.toList());
        log.info("Parsed " + sourceFiles.size() + " pom.xml files in the root path: " + ROOT_PATH);
    }

    static final List<Class> recipesToExpose = List.of(
        // Add
        AddDependency.class,
        AddManagedDependency.class,
        AddParentPom.class,
        AddPlugin.class,
        AddPluginDependency.class,
        AddProperty.class,
        // Change
        ChangeDependencyGroupIdAndArtifactId.class,
        ChangeManagedDependencyGroupIdAndArtifactId.class,
        ChangeParentPom.class,
        ChangePluginConfiguration.class,
        ChangePluginGroupIdAndArtifactId.class,
        // Remove
        RemoveDependency.class,
        RemoveDuplicateDependencies.class,
        RemoveManagedDependency.class,
        RemovePlugin.class,
        RemovePluginDependency.class,
        RemoveProperty.class,
        // Update
        UpdateMavenProjectPropertyJavaVersion.class,
        UpdateMavenWrapper.class,
        UpgradeDependencyVersion.class,
        UpgradeParentVersion.class,
        UpgradeParentVersion.class,
        UpgradePluginVersion.class,
        // Other
        OrderPomElements.class
    );

    @Tool(name = "list_all_available_maven_migration_tools", description = "Lists of the available Maven migration tools.")
    public ToolResponse listAllTheAvailableMavenMigrationTools() throws JsonProcessingException {
        log.info("List all the " + recipesToExpose.size() + " available Maven Migration Tools");
        return ToolResponse.success(getRecipeAsJson());
    }

    @Tool(name = "add_dependency", description = "Add a Maven dependency to a `pom.xml` file in the correct scope based on where it is used.")
    public ToolResponse executeAddDependencyRecipe(
        @ToolArg(name = "Group ID", description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.") String groupId,
        @ToolArg(name = "Artifact ID", description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.") String artifactId,
        @ToolArg(name = "Version", description = "An exact version number or node-style semver selector used to select the version number.") String version,
        @ToolArg(name = "Scope", description = "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but is used when adding a runtime, provided, or import dependency.", required = false) String scope) throws IOException {
        log.infov("Execute AddDependency Recipe ({0}, {1}, {2}, {3})", groupId, artifactId, version, scope);
        AddDependency addDependency = new AddDependency(groupId, artifactId, version, null, scope, null, null, null, null, null, null, null);
        return executeRecipe(addDependency);
    }

    private static ToolResponse executeRecipe(Recipe recipe) throws IOException {
        // Apply the recipe
        RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles), executionContext);

        // Process results
        List<Result> results = recipeRun.getChangeset().getAllResults();
        for (Result result : results) {
            // Write the changes back to disk
            Path absolutePath = ROOT_PATH.resolve(result.getBefore().getSourcePath());
            Files.writeString(absolutePath, result.getAfter().printAll());
        }

        if (results.isEmpty()) {
            log.info("Executing the tool " + recipe.getDisplayName() + " made no change in the code");
            return ToolResponse.success("Executing the tool " + recipe.getDisplayName() + " made no change in the code located in " + ROOT_APP_TO_MIGRATE);
        } else {
            log.info("Executing the tool " + recipe.getDisplayName() + " made " + results.size() + " changes in the code");
            return ToolResponse.success("Executing the tool " + recipe.getDisplayName() + " made " + results.size() + " changes in the code located in " + ROOT_APP_TO_MIGRATE);
        }
    }

    String getRecipeAsJson() {
        return """
            [
              {
                "migration": "Maven Migration",
                "fqn": "org.openrewrite.maven.AddDependency",
                "name": "add_dependency",
                "displayName": "Add Maven dependency",
                "description": "Add a Maven dependency to a `pom.xml` file in the correct scope based on where it is used.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "version",
                    "displayName": "Version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "scope",
                    "displayName": "Scope",
                    "description": "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but is used when adding a runtime, provided, or import dependency.",
                    "type": "String"
                  },
                  {
                    "name": "optional",
                    "displayName": "Optional",
                    "description": "Set the value of the `<optional>` tag. No `<optional>` tag will be added when this is `null`.",
                    "type": "Boolean"
                  },
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.AddManagedDependency",
                "name": "add_managed_dependency",
                "displayName": "Add managed Maven dependency",
                "description": "Add a managed Maven dependency to a `pom.xml` file.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate 'org.apache.logging.log4j:ARTIFACT_ID:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate 'org.apache.logging.log4j:log4j-bom:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "version",
                    "displayName": "Version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "scope",
                    "displayName": "Scope",
                    "description": "An optional scope to use for the dependency management tag.",
                    "type": "String"
                  },
                  {
                    "name": "type",
                    "displayName": "Type",
                    "description": "An optional type to use for the dependency management tag.",
                    "type": "String"
                  },
                  {
                    "name": "classifier",
                    "displayName": "Classifier",
                    "description": "An optional classifier to use for the dependency management tag",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "releases_only",
                    "displayName": "Releases only",
                    "description": "Whether to exclude snapshots from consideration when using a semver selector",
                    "type": "Boolean"
                  },
                  {
                    "name": "only_if_using",
                    "displayName": "Only if using glob expression for group:artifact",
                    "description": "Only add managed dependencies to projects having a dependency matching the expression.",
                    "type": "String"
                  },
                  {
                    "name": "add_to_root_pom",
                    "displayName": "Add to the root pom",
                    "description": "Add to the root pom where root is the eldest parent of the pom within the source set.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.AddParentPom",
                "name": "add_parent_pom",
                "displayName": "Add Maven parent",
                "description": "Add a parent pom to a Maven pom.xml. Does nothing if a parent pom is already present.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group ID",
                    "description": "The group ID of the maven parent pom to be adopted.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact ID",
                    "description": "The artifact ID of the maven parent pom to be adopted.",
                    "type": "String"
                  },
                  {
                    "name": "version",
                    "displayName": "Version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "relative_path",
                    "displayName": "Relative path",
                    "description": "New relative path attribute for parent lookup.",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.AddPlugin",
                "name": "add_plugin",
                "displayName": "Add Maven plugin",
                "description": "Add the specified Maven plugin to the pom.xml.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "version",
                    "displayName": "Version",
                    "description": "A fixed version of the plugin to add.",
                    "type": "String"
                  },
                  {
                    "name": "configuration",
                    "displayName": "Configuration",
                    "description": "Optional plugin configuration provided as raw XML",
                    "type": "String"
                  },
                  {
                    "name": "dependencies",
                    "displayName": "Dependencies",
                    "description": "Optional plugin dependencies provided as raw XML.",
                    "type": "String"
                  },
                  {
                    "name": "executions",
                    "displayName": "Executions",
                    "description": "Optional executions provided as raw XML.",
                    "type": "String"
                  },
                  {
                    "name": "file_pattern",
                    "displayName": "File pattern",
                    "description": "A glob expression that can be used to constrain which directories or source files should be searched. Multiple patterns may be specified, separated by a semicolon `;`. If multiple patterns are supplied any of the patterns matching will be interpreted as a match. When not set, all source files are searched. ",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.AddPluginDependency",
                "name": "add_plugin_dependency",
                "displayName": "Add Maven plugin dependencies",
                "description": "Adds the specified dependencies to a Maven plugin. Will not add the plugin if it does not already exist in the pom.",
                "options": [
                  {
                    "name": "plugin_group_id",
                    "displayName": "Plugin group",
                    "description": "Group ID of the plugin to which the dependency will be added. A group ID is the first part of a dependency coordinate `org.openrewrite.maven:rewrite-maven-plugin:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "plugin_artifact_id",
                    "displayName": "Plugin artifact",
                    "description": "Artifact ID of the plugin to which the dependency will be added.The second part of a dependency coordinate `org.openrewrite.maven:rewrite-maven-plugin:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The group ID of the dependency to add.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The artifact ID of the dependency to add.",
                    "type": "String"
                  },
                  {
                    "name": "version",
                    "displayName": "Version",
                    "description": "The version of the dependency to add.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.AddProperty",
                "name": "add_property",
                "displayName": "Add Maven project property",
                "description": "Add a new property to the Maven project property. Prefers to add the property to the parent if the project has multiple modules.",
                "options": [
                  {
                    "name": "key",
                    "displayName": "Key",
                    "description": "The name of the property key to be added.",
                    "type": "String"
                  },
                  {
                    "name": "value",
                    "displayName": "Value",
                    "description": "The value of property to be added.",
                    "type": "String"
                  },
                  {
                    "name": "preserve_existing_value",
                    "displayName": "Preserve existing value",
                    "description": "Preserve previous value if the property already exists in the pom file.",
                    "type": "Boolean"
                  },
                  {
                    "name": "trust_parent",
                    "displayName": "Trust parent POM",
                    "description": "If the parent defines a property with the same key, trust it even if the value isn't the same. Useful when you want to wait for the parent to have its value changed first. The parent is not trusted by default.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId",
                "name": "change_dependency_group_id_and_artifact_id",
                "displayName": "Change Maven dependency",
                "description": "Change a Maven dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before. Matching `<dependencyManagement>` coordinates are also updated if a `newVersion` or `versionPattern` is provided.",
                "options": [
                  {
                    "name": "old_group_id",
                    "displayName": "Old groupId",
                    "description": "The old groupId to replace. The groupId is the first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
                    "type": "String"
                  },
                  {
                    "name": "old_artifact_id",
                    "displayName": "Old artifactId",
                    "description": "The old artifactId to replace. The artifactId is the second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
                    "type": "String"
                  },
                  {
                    "name": "new_group_id",
                    "displayName": "New groupId",
                    "description": "The new groupId to use. Defaults to the existing group id.",
                    "type": "String"
                  },
                  {
                    "name": "new_artifact_id",
                    "displayName": "New artifactId",
                    "description": "The new artifactId to use. Defaults to the existing artifact id.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "override_managed_version",
                    "displayName": "Override managed version",
                    "description": "If the new dependency has a managed version, this flag can be used to explicitly set the version on the dependency. The default for this flag is `false`.",
                    "type": "Boolean"
                  },
                  {
                    "name": "change_managed_dependency",
                    "displayName": "Update dependency management",
                    "description": "Also update the dependency management section. The default for this flag is `true`.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId",
                "name": "change_managed_dependency_group_id_and_artifact_id",
                "displayName": "Change Maven managed dependency groupId, artifactId and optionally the version",
                "description": "Change the groupId, artifactId and optionally the version of a specified Maven managed dependency.",
                "options": [
                  {
                    "name": "old_group_id",
                    "displayName": "Old groupId",
                    "description": "The old groupId to replace. The groupId is the first part of a managed dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "old_artifact_id",
                    "displayName": "Old artifactId",
                    "description": "The old artifactId to replace. The artifactId is the second part of a managed dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "new_group_id",
                    "displayName": "New groupId",
                    "description": "The new groupId to use.",
                    "type": "String"
                  },
                  {
                    "name": "new_artifact_id",
                    "displayName": "New artifactId",
                    "description": "The new artifactId to use.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "The new version to use.",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.ChangeParentPom",
                "name": "change_parent_pom",
                "displayName": "Change Maven parent",
                "description": "Change the parent pom of a Maven pom.xml. Identifies the parent pom to be changed by its groupId and artifactId.",
                "options": [
                  {
                    "name": "old_group_id",
                    "displayName": "Old group ID",
                    "description": "The group ID of the Maven parent pom to be changed away from.",
                    "type": "String"
                  },
                  {
                    "name": "new_group_id",
                    "displayName": "New group ID",
                    "description": "The group ID of the new maven parent pom to be adopted. If this argument is omitted it defaults to the value of `oldGroupId`.",
                    "type": "String"
                  },
                  {
                    "name": "old_artifact_id",
                    "displayName": "Old artifact ID",
                    "description": "The artifact ID of the maven parent pom to be changed away from.",
                    "type": "String"
                  },
                  {
                    "name": "new_artifact_id",
                    "displayName": "New artifact ID",
                    "description": "The artifact ID of the new maven parent pom to be adopted. If this argument is omitted it defaults to the value of `oldArtifactId`.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "old_relative_path",
                    "displayName": "Old relative path",
                    "description": "The relativePath of the maven parent pom to be changed away from. Use an empty String to match `<relativePath />`, use `../pom.xml` to match the default value.",
                    "type": "String"
                  },
                  {
                    "name": "new_relative_path",
                    "displayName": "New relative path",
                    "description": "New relative path attribute for parent lookup.",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "allow_version_downgrades",
                    "displayName": "Allow version downgrades",
                    "description": "If the new parent has the same group/artifact, this flag can be used to only upgrade the version if the target version is newer than the current.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.ChangePluginConfiguration",
                "name": "change_plugin_configuration",
                "displayName": "Change Maven plugin configuration",
                "description": "Apply the specified configuration to a Maven plugin. Will not add the plugin if it does not already exist in the pom.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of the coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the plugin to modify.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION' of the plugin to modify.",
                    "type": "String"
                  },
                  {
                    "name": "configuration",
                    "displayName": "Configuration",
                    "description": "Plugin configuration provided as raw XML overriding any existing configuration. Configuration inside `<executions>` blocks will not be altered. Supplying `null` will remove any existing configuration.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.ChangePluginGroupIdAndArtifactId",
                "name": "change_plugin_group_id_and_artifact_id",
                "displayName": "Change Maven plugin group and artifact ID",
                "description": "Change the groupId and/or the artifactId of a specified Maven plugin. Optionally update the plugin version.",
                "options": [
                  {
                    "name": "old_group_id",
                    "displayName": "Old group ID",
                    "description": "The old group ID to replace. The group ID is the first part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
                    "type": "String"
                  },
                  {
                    "name": "old_artifact_id",
                    "displayName": "Old artifact ID",
                    "description": "The old artifactId to replace. The artifact ID is the second part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
                    "type": "String"
                  },
                  {
                    "name": "new_group_id",
                    "displayName": "New group ID",
                    "description": "The new group ID to use. Defaults to the existing group ID.",
                    "type": "String"
                  },
                  {
                    "name": "new_artifact_id",
                    "displayName": "New artifact ID",
                    "description": "The new artifact ID to use. Defaults to the existing artifact ID.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.RemoveDependency",
                "name": "remove_dependency",
                "displayName": "Remove Maven dependency",
                "description": "Removes a single dependency from the <dependencies> section of the pom.xml.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "scope",
                    "displayName": "Scope",
                    "description": "Only remove dependencies if they are in this scope. If 'runtime', this willalso remove dependencies in the 'compile' scope because 'compile' dependencies are part of the runtime dependency set",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.RemoveDuplicateDependencies",
                "name": "remove_duplicate_dependencies",
                "displayName": "Remove duplicate Maven dependencies",
                "description": "Removes duplicated dependencies in the `<dependencies>` and `<dependencyManagement>` sections of the `pom.xml`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.RemoveManagedDependency",
                "name": "remove_managed_dependency",
                "displayName": "Remove Maven managed dependency",
                "description": "Removes a single managed dependency from the <dependencyManagement><dependencies> section of the pom.xml.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a managed dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a managed dependency coordinate `com.google.guava:guava:VERSION`.",
                    "type": "String"
                  },
                  {
                    "name": "scope",
                    "displayName": "Scope",
                    "description": "Only remove managed dependencies if they are in this scope. If `runtime`, this will also remove managed dependencies in the 'compile' scope because `compile` dependencies are part of the runtime dependency set.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.RemovePlugin",
                "name": "remove_plugin",
                "displayName": "Remove Maven plugin",
                "description": "Remove the specified Maven plugin from the POM.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.RemovePluginDependency",
                "name": "remove_plugin_dependency",
                "displayName": "Remove Maven plugin dependency",
                "description": "Removes a dependency from the <dependencies> section of a plugin in the pom.xml.",
                "options": [
                  {
                    "name": "plugin_group_id",
                    "displayName": "Plugin group ID",
                    "description": "Group ID of the plugin from which the dependency will be removed. Supports glob.A Group ID is the first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "plugin_artifact_id",
                    "displayName": "Plugin artifact ID",
                    "description": "Artifact ID of the plugin from which the dependency will be removed. Supports glob.The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a plugin dependency coordinate. Supports glob.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a plugin dependency coordinate. Supports glob.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.RemoveProperty",
                "name": "remove_property",
                "displayName": "Remove Maven project property",
                "description": "Removes the specified Maven project property from the pom.xml.",
                "options": [
                  {
                    "name": "property_name",
                    "displayName": "Property name",
                    "description": "Key name of the property to remove.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.UpdateMavenProjectPropertyJavaVersion",
                "name": "update_maven_project_property_java_version",
                "displayName": "Update Maven Java project properties",
                "description": "The Java version is determined by several project properties, including:\\n\\n * `java.version`\\n * `jdk.version`\\n * `javaVersion`\\n * `jdkVersion`\\n * `maven.compiler.source`\\n * `maven.compiler.target`\\n * `maven.compiler.release`\\n * `release.version`\\n\\nIf none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.",
                "options": [
                  {
                    "name": "version",
                    "displayName": "Java version",
                    "description": "The Java version to upgrade to.",
                    "type": "Integer"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.UpdateMavenWrapper",
                "name": "update_maven_wrapper",
                "displayName": "Update Maven wrapper",
                "description": "Update the version of Maven used in an existing Maven wrapper.",
                "options": [
                  {
                    "name": "wrapper_version",
                    "displayName": "New wrapper version",
                    "description": "An exact version number or node-style semver selector used to select the wrapper version number.",
                    "type": "String"
                  },
                  {
                    "name": "wrapper_distribution",
                    "displayName": "Wrapper Distribution type",
                    "description": "The distribution of the Maven wrapper to use.\\n\\n* \\"bin\\" uses a `maven-wrapper.jar` compiled binary.\\n* \\"only-script\\" uses a lite version of `mvnw`/`mvnw.cmd` using wget/curl or powershell. (required wrapper 3.2.0 or newer)\\n* \\"script\\" downloads `maven-wrapper.jar` or `MavenWrapperDownloader.java` to then download a full distribution.\\n* \\"source\\" uses `MavenWrapperDownloader.java` source file.\\n\\nDefaults to \\"bin\\".",
                    "type": "String"
                  },
                  {
                    "name": "distribution_version",
                    "displayName": "New distribution version",
                    "description": "An exact version number or node-style semver selector used to select the Maven version number.",
                    "type": "String"
                  },
                  {
                    "name": "repository_url",
                    "displayName": "Repository URL",
                    "description": "The URL of the repository to download the Maven wrapper and distribution from. Supports repositories with a Maven layout. Defaults to `https://repo.maven.apache.org/maven2`.",
                    "type": "String"
                  },
                  {
                    "name": "add_if_missing",
                    "displayName": "Add if missing",
                    "description": "Add a Maven wrapper, if it's missing. Defaults to `true`.",
                    "type": "Boolean"
                  },
                  {
                    "name": "enforce_wrapper_checksum_verification",
                    "displayName": "Enforce checksum verification for maven-wrapper.jar",
                    "description": "Enforce checksum verification for the maven-wrapper.jar. Enabling this feature may sporadically result in build failures, such as [MWRAPPER-103](https://issues.apache.org/jira/browse/MWRAPPER-103). Defaults to `false`.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.UpgradeDependencyVersion",
                "name": "upgrade_dependency_version",
                "displayName": "Upgrade Maven dependency version",
                "description": "Upgrade the version of a dependency by specifying a group and (optionally) an artifact using Node Semver advanced range selectors, allowing more precise control over version updates to patch or minor releases.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number. You can also use `latest.release` for the latest available version and `latest.patch` if the current version is a valid semantic version. For more details, you can look at the documentation page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors)",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'newVersion' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "override_managed_version",
                    "displayName": "Override managed version",
                    "description": "This flag can be set to explicitly override a managed dependency's version. The default for this flag is `false`.",
                    "type": "Boolean"
                  },
                  {
                    "name": "retain_versions",
                    "displayName": "Retain versions",
                    "description": "Accepts a list of GAVs. For each GAV, if it is a project direct dependency, and it is removed from dependency management after the changes from this recipe, then it will be retained with an explicit version. The version can be omitted from the GAV to use the old value from dependency management",
                    "type": "List"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.UpgradeParentVersion",
                "name": "upgrade_parent_version",
                "displayName": "Upgrade Maven parent project version",
                "description": "Set the parent pom version number according to a [version selector](https://docs.openrewrite.org/reference/dependency-version-selectors) or to a specific version number.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate 'org.springframework.boot:spring-boot-parent:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate 'org.springframework.boot:spring-boot-parent:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "only_external",
                    "displayName": "Only external",
                    "description": "Only upgrade `<parent>` if external to the project, i.e. it has an empty `<relativePath>`. Defaults to `false`.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.UpgradeParentVersion",
                "name": "upgrade_parent_version",
                "displayName": "Upgrade Maven parent project version",
                "description": "Set the parent pom version number according to a [version selector](https://docs.openrewrite.org/reference/dependency-version-selectors) or to a specific version number.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate 'org.springframework.boot:spring-boot-parent:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate 'org.springframework.boot:spring-boot-parent:VERSION'.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number.",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "only_external",
                    "displayName": "Only external",
                    "description": "Only upgrade `<parent>` if external to the project, i.e. it has an empty `<relativePath>`. Defaults to `false`.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.UpgradePluginVersion",
                "name": "upgrade_plugin_version",
                "displayName": "Upgrade Maven plugin version",
                "description": "Upgrade the version of a plugin using Node Semver advanced range selectors, allowing more precise control over version updates to patch or minor releases.",
                "options": [
                  {
                    "name": "group_id",
                    "displayName": "Group",
                    "description": "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'. Supports globs.",
                    "type": "String"
                  },
                  {
                    "name": "artifact_id",
                    "displayName": "Artifact",
                    "description": "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'. Supports globs.",
                    "type": "String"
                  },
                  {
                    "name": "new_version",
                    "displayName": "New version",
                    "description": "An exact version number or node-style semver selector used to select the version number. You can also use `latest.release` for the latest available version and `latest.patch` if the current version is a valid semantic version. For more details, you can look at the documentation page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors)",
                    "type": "String"
                  },
                  {
                    "name": "version_pattern",
                    "displayName": "Version pattern",
                    "description": "Allows version selection to be extended beyond the original Node Semver semantics. So for example,Setting 'version' to \\"25-29\\" can be paired with a metadata pattern of \\"-jre\\" to select Guava 29.0-jre",
                    "type": "String"
                  },
                  {
                    "name": "trust_parent",
                    "displayName": "Trust parent POM",
                    "description": "Even if the parent suggests a version that is older than what we are trying to upgrade to, trust it anyway. Useful when you want to wait for the parent to catch up before upgrading. The parent is not trusted by default.",
                    "type": "Boolean"
                  },
                  {
                    "name": "add_version_if_missing",
                    "displayName": "Add version if missing",
                    "description": "If the plugin is missing a version, add the latest release. Defaults to false.",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.maven.OrderPomElements",
                "name": "order_pom_elements",
                "displayName": "Order POM elements",
                "description": "Order POM elements according to the [recommended](http://maven.apache.org/developers/conventions/code.html#pom-code-convention) order.",
                "options": []
              }
            ]
            """;
    }

    String getRecipeJson() throws JsonProcessingException {

        List<RecipeJson> jsonRecipes = new ArrayList<>();
        for (Class recipeClass : recipesToExpose) {

            Recipe recipe = RecipeIntrospectionUtils.constructRecipe(recipeClass);

            List<OptionJson> jsonOptions = new ArrayList<>();
            for (OptionDescriptor optionDescriptor : recipe.getDescriptor().getOptions()) {
                jsonOptions.add(new OptionJson(camelToSnakeCase(optionDescriptor.getName()), optionDescriptor.getDisplayName(), optionDescriptor.getDescription(), optionDescriptor.getType()));
            }

            jsonRecipes.add(new RecipeJson("Maven Migration", recipe.getName(), camelToSnakeCase(recipe.getClass().getSimpleName()), recipe.getDisplayName(), recipe.getDescription(), jsonOptions));
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(jsonRecipes);
    }

    static String camelToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));

        for (int i = 1; i < camelCase.length(); i++) {
            char currentChar = camelCase.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                result.append('_');
                result.append(Character.toLowerCase(currentChar));
            } else {
                result.append(currentChar);
            }
        }

        return result.toString();
    }

    private static void collectPomXMLFiles(File directory, List<Path> files) {
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    collectPomXMLFiles(file, files);
                } else if (file.getName().equals("pom.xml")) {
                    files.add(file.toPath());
                }
            }
        }
    }
}

