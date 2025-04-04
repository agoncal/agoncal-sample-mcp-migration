package openrewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.migrate.net.URLConstructorToURICreate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MigrationOpenRewriteMCPServerTest {

    public static void main(String[] args) throws IOException {

        // Create execution context
        ExecutionContext executionContext = new InMemoryExecutionContext(t -> t.printStackTrace());

        // Create Java parser
        JavaParser javaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build();

        // Specify source files to process
        Path sourceFilePath = Paths.get("/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration");

        // Recursively find all Java files in the directory and its subdirectories
        List<Path> sourcePaths = findJavaFiles(sourceFilePath.toFile());

        // Parse the source files
        List<SourceFile> sourceFiles = javaParser.parse(sourcePaths, sourceFilePath, executionContext).collect(Collectors.toList());

        // Create and configure the recipe
        Recipe recipe = new URLConstructorToURICreate();

        // Apply the recipe
        RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles).generate(sourceFiles), executionContext);

        // Process results
        List<Result> results = recipeRun.getChangeset().getAllResults();
        for (Result result : results) {
            System.out.println("Changed: " + result.getAfter().printAll());

            // Write the changes back to disk
            Files.writeString(result.getBefore().getSourcePath(), result.getAfter().printAll());
        }
    }

    // Helper method to recursively find all Java files
    private static List<Path> findJavaFiles(File directory) {
        List<Path> files = new ArrayList<>();
        if (directory.exists()) {
            collectJavaFiles(directory, files);
        } else {
            System.err.println("Directory does not exist: " + directory);
        }
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

