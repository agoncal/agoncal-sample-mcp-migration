package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.McpLog;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolResponse;
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.migrate.BeansXmlNamespace;
import org.openrewrite.java.migrate.CastArraysAsListToList;
import org.openrewrite.java.migrate.ChangeDefaultKeyStore;
import org.openrewrite.java.migrate.IllegalArgumentExceptionToAlreadyConnectedException;
import org.openrewrite.java.migrate.JREThrowableFinalMethods;
import org.openrewrite.java.migrate.RemovedSecurityManagerMethods;
import org.openrewrite.java.migrate.ReplaceComSunAWTUtilitiesMethods;
import org.openrewrite.java.migrate.UpgradeJavaVersion;
import org.openrewrite.java.migrate.UseJavaUtilBase64;
import org.openrewrite.java.migrate.io.ReplaceFileInOrOutputStreamFinalizeWithClose;
import org.openrewrite.java.migrate.jakarta.ApplicationPathWildcardNoLongerAccepted;
import org.openrewrite.java.migrate.jakarta.RemoveBeanIsNullable;
import org.openrewrite.java.migrate.jakarta.UpdateAnnotationAttributeJavaxToJakarta;
import org.openrewrite.java.migrate.jakarta.UpdateBeanManagerMethods;
import org.openrewrite.java.migrate.jakarta.UpdateGetRealPath;
import org.openrewrite.java.migrate.javax.AddColumnAnnotation;
import org.openrewrite.java.migrate.javax.AddDefaultConstructorToEntityClass;
import org.openrewrite.java.migrate.javax.AddJaxwsRuntime;
import org.openrewrite.java.migrate.javax.RemoveTemporalAnnotation;
import org.openrewrite.java.migrate.lang.StringFormatted;
import org.openrewrite.java.migrate.lang.ThreadStopUnsupported;
import org.openrewrite.java.migrate.lang.UseStringIsEmptyRecipe;
import org.openrewrite.java.migrate.lang.UseTextBlocks;
import org.openrewrite.java.migrate.logging.MigrateLogRecordSetMillisToSetInstant;
import org.openrewrite.java.migrate.logging.MigrateLoggerGlobalToGetGlobal;
import org.openrewrite.java.migrate.net.MigrateURLDecoderDecode;
import org.openrewrite.java.migrate.net.MigrateURLEncoderEncode;
import org.openrewrite.java.migrate.net.URLConstructorToURICreate;
import org.openrewrite.java.migrate.net.URLConstructorsToNewURI;
import org.openrewrite.java.migrate.sql.MigrateDriverManagerSetLogStream;
import org.openrewrite.java.migrate.util.IteratorNext;
import org.openrewrite.java.migrate.util.ListFirstAndLast;
import org.openrewrite.java.migrate.util.MigrateCollectionsSingletonList;
import org.openrewrite.java.migrate.util.MigrateCollectionsSingletonMap;
import org.openrewrite.java.migrate.util.MigrateCollectionsUnmodifiableList;
import org.openrewrite.java.migrate.util.UseEnumSetOf;
import org.openrewrite.java.migrate.util.UseLocaleOf;
import org.openrewrite.java.migrate.util.UseMapOf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MigrationOpenRewriteMCPServer {

    private static final Logger log = Logger.getLogger(MigrationOpenRewriteMCPServer.class);
    public static final String ROOT_PATH = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration";

    static final List<Class> recipesToExpose = List.of(
        BeansXmlNamespace.class,
        CastArraysAsListToList.class,
        ChangeDefaultKeyStore.class,
        IllegalArgumentExceptionToAlreadyConnectedException.class,
        JREThrowableFinalMethods.class,
        RemovedSecurityManagerMethods.class,
        ReplaceComSunAWTUtilitiesMethods.class,
        UpgradeJavaVersion.class,
        UseJavaUtilBase64.class,
        ThreadStopUnsupported.class,
        ReplaceFileInOrOutputStreamFinalizeWithClose.class,
        ApplicationPathWildcardNoLongerAccepted.class,
        RemoveBeanIsNullable.class,
        UpdateAnnotationAttributeJavaxToJakarta.class,
        UpdateBeanManagerMethods.class,
        UpdateGetRealPath.class,
        AddColumnAnnotation.class,
        URLConstructorToURICreate.class,
        AddDefaultConstructorToEntityClass.class,
        AddJaxwsRuntime.class,
        RemoveTemporalAnnotation.class,
        StringFormatted.class,
        UseStringIsEmptyRecipe.class,
        UseTextBlocks.class,
        MigrateLoggerGlobalToGetGlobal.class,
        MigrateLogRecordSetMillisToSetInstant.class,
        MigrateURLDecoderDecode.class,
        MigrateURLEncoderEncode.class,
        URLConstructorsToNewURI.class,
        MigrateDriverManagerSetLogStream.class,
        IteratorNext.class,
        ListFirstAndLast.class,
        MigrateCollectionsSingletonList.class,
        MigrateCollectionsSingletonMap.class,
        MigrateCollectionsUnmodifiableList.class,
        UseEnumSetOf.class,
        UseLocaleOf.class,
        UseMapOf.class
    );

    @Tool(name = "url_constructor_to_uri_create", description = "Converts `new URL(String)` constructor to `URI.create(String).toURL()`.")
    public ToolResponse executeURLConstructorToURICreateRecipe( ) throws IOException {
        log.info("Execute URLConstructorToURICreate Recipe");

        // Create execution context
        ExecutionContext executionContext = new InMemoryExecutionContext(t -> t.printStackTrace());

        // Create Java parser
        JavaParser javaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build();

        // Specify source files to process
        Path sourceFilePath = Paths.get(ROOT_PATH);

        // Recursively find all Java files in the directory and its subdirectories
        List<Path> sourcePaths = findJavaFiles(sourceFilePath.toFile());

        // Parse the source files
        List<SourceFile> sourceFiles = javaParser.parse(sourcePaths, sourceFilePath, executionContext).collect(Collectors.toList());

        // Create and configure the recipe
        Recipe recipe = RecipeIntrospectionUtils.constructRecipe(URLConstructorToURICreate.class);

        // Apply the recipe
        RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles).generate(sourceFiles), executionContext);

        // Process results
        List<Result> results = recipeRun.getChangeset().getAllResults();
        for (Result result : results) {
            // Write the changes back to disk
            Path absolutePath = Paths.get(ROOT_PATH).resolve(result.getBefore().getSourcePath());
            Files.writeString(absolutePath, result.getAfter().printAll());
        }

        return ToolResponse.success("Executing " + recipe.getDisplayName() + " made " + results.size() + " changes in the code located in " + ROOT_PATH);
    }

    @Tool(name = "string_formatted", description = "Prefer `String.formatted(Object...)` over `String.format(String, Object...)` in Java 17 or higher.")
    public ToolResponse executeStringFormattedRecipe(McpLog mcpLog) throws IOException {
        log.info("Execute StringFormatted Recipe");

        // Create execution context
        ExecutionContext executionContext = new InMemoryExecutionContext(t -> t.printStackTrace());

        // Create Java parser
        JavaParser javaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build();

        // Specify source files to process
        Path sourceFilePath = Paths.get(ROOT_PATH);

        // Recursively find all Java files in the directory and its subdirectories
        List<Path> sourcePaths = findJavaFiles(sourceFilePath.toFile());

        // Parse the source files
        List<SourceFile> sourceFiles = javaParser.parse(sourcePaths, sourceFilePath, executionContext).collect(Collectors.toList());

        // Create and configure the recipe
        Recipe recipe = RecipeIntrospectionUtils.constructRecipe(StringFormatted.class);

        // Apply the recipe
        RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles).generate(sourceFiles), executionContext);

        // Process results
        List<Result> results = recipeRun.getChangeset().getAllResults();
        for (Result result : results) {
            // Write the changes back to disk
            Files.writeString(result.getBefore().getSourcePath(), result.getAfter().printAll());
        }

        return ToolResponse.success("Executing " + recipe.getDisplayName() + " made " + results.size() + " changes in the code located in " + ROOT_PATH);
    }

    @Tool(name = "list_all_available_openrewrite_recipes", description = "Lists of the available OpenRewrite recipes.")
    public ToolResponse listAllTheAvailableOpenRewriteRecipes() throws JsonProcessingException {
        log.info("List All The Available OpenRewrite Recipes");

        return ToolResponse.success(getRecipeAsJson());
    }

    static String getRecipeAsJson() throws JsonProcessingException {

        List<RecipeJson> jsonRecipes = new ArrayList<>();
        for (Class recipeClass : recipesToExpose) {

            Recipe recipe = RecipeIntrospectionUtils.constructRecipe(recipeClass);

            List<OptionJson> jsonOptions = new ArrayList<>();
            for (OptionDescriptor optionDescriptor : recipe.getDescriptor().getOptions()) {
                jsonOptions.add(new OptionJson(camelToSnakeCase(optionDescriptor.getName()), optionDescriptor.getDisplayName(), optionDescriptor.getDescription(), optionDescriptor.getType()));
            }

            jsonRecipes.add(new RecipeJson("Java Migration", recipe.getName(), camelToSnakeCase(recipe.getClass().getSimpleName()), recipe.getDisplayName(), recipe.getDescription(), jsonOptions));
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

    // Helper method to recursively find all Java files
    private static List<Path> findJavaFiles(File directory) {
        log.info("Finding the number of Java files in the directory: " + directory);
        List<Path> files = new ArrayList<>();
        if (directory.exists()) {
            collectJavaFiles(directory, files);
        } else {
            System.err.println("Directory does not exist: " + directory);
        }

        log.info("Found " + files.size() + " Java files in the directory: " + directory);
        return files;
    }

    private static void collectJavaFiles(File directory, List<Path> files) {
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    collectJavaFiles(file, files);
                } else if (file.getName().endsWith(".java")) {
                    files.add(file.toPath());
                }
            }
        }
    }
}

record RecipeJson(String migration, String fqn, String name, String displayName, String description,
                  List<OptionJson> options) {
}

record OptionJson(String name, String displayName, String description, String type) {
}

