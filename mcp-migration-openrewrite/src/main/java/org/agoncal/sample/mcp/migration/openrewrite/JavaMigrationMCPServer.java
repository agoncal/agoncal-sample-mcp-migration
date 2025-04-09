package org.agoncal.sample.mcp.migration.openrewrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
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

public class JavaMigrationMCPServer {

    private static final Logger log = Logger.getLogger(JavaMigrationMCPServer.class);
    private static final String ROOT = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration/mcp-migration-legacy";
    private static final Path ROOT_PATH = Paths.get(ROOT);
    private static final File ROOT_DIRECTORY = Paths.get(ROOT).toFile();
    private static List<Path> JAVA_FILES;

    @PostConstruct
    void findJavaFiles() {
        log.info("Finding the number of Java files in the directory: " + ROOT);
        JAVA_FILES = new ArrayList<>();
        if (ROOT_DIRECTORY.exists()) {
            collectJavaFiles(ROOT_DIRECTORY, JAVA_FILES);
        } else {
            System.err.println("Directory does not exist: " + ROOT_DIRECTORY);
        }

        log.info("Found " + JAVA_FILES.size() + " Java files in the directory: " + ROOT_DIRECTORY);
    }

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

    @Tool(name = "beans_xml_namespace", description = "Change `beans.xml` `schemaLocation` to match XML namespace. Set the `schemaLocation` that corresponds to the `xmlns` set in `beans.xml` files.")
    public ToolResponse executeBeansXmlNamespaceRecipe() throws IOException {
        log.info("Execute BeansXmlNamespace Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(BeansXmlNamespace.class));
    }

    @Tool(name = "cast_arrays_as_list_to_list", description = "Remove explicit casts on `Arrays.asList(..).toArray()`. Convert code like `(Integer[]) Arrays.asList(1, 2, 3).toArray()` to `Arrays.asList(1, 2, 3).toArray(new Integer[0])`.")
    public ToolResponse executeCastArraysAsListToListRecipe() throws IOException {
        log.info("Execute CastArraysAsListToList Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(CastArraysAsListToList.class));
    }

    @Tool(name = "change_default_key_store", description = "Return String `jks` when  `KeyStore.getDefaultType()` is called. In Java 11 the default keystore was updated from JKS to PKCS12. As a result, applications relying on KeyStore.getDefaultType() may encounter issues after migrating, unless their JKS keystore has been converted to PKCS12. This recipe returns default key store of `jks` when `KeyStore.getDefaultType()` method is called to use the pre Java 11 default keystore.")
    public ToolResponse executeChangeDefaultKeyStoreRecipe() throws IOException {
        log.info("Execute ChangeDefaultKeyStore Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(ChangeDefaultKeyStore.class));
    }

    @Tool(name = "illegal_argument_exception_to_already_connected_exception", description = "Replace `IllegalArgumentException` with `AlreadyConnectedException` for DatagramChannel.send() to ensure compatibility with Java 11+.")
    public ToolResponse executeIllegalArgumentExceptionToAlreadyConnectedExceptionRecipe() throws IOException {
        log.info("Execute IllegalArgumentExceptionToAlreadyConnectedException Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(IllegalArgumentExceptionToAlreadyConnectedException.class));
    }

    @Tool(name = "jre_throwable_final_methods", description = "Rename final method declarations `getSuppressed()` and `addSuppressed(Throwable exception)` in classes that extend `Throwable`")
    public ToolResponse executeJREThrowableFinalMethodsRecipe() throws IOException {
        log.info("Execute JREThrowableFinalMethods Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(JREThrowableFinalMethods.class));
    }

    @Tool(name = "removed_security_manager_methods", description = "Replace deprecated methods in`SecurityManager`. Replace `SecurityManager` methods `checkAwtEventQueueAccess()`, `checkSystemClipboardAccess()`, `checkMemberAccess()` and `checkTopLevelWindow()` deprecated in Java SE 11 by `checkPermission(new java.security.AllPermission())`.")
    public ToolResponse executeRemovedSecurityManagerMethodsRecipe() throws IOException {
        log.info("Execute RemovedSecurityManagerMethods Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(RemovedSecurityManagerMethods.class));
    }

    @Tool(name = "thread_stop_unsupported", description = "Replace `Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` with `throw new UnsupportedOperationException()`")
    public ToolResponse executeThreadStopUnsupportedRecipe() throws IOException {
        log.info("Execute ThreadStopUnsupported Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(ThreadStopUnsupported.class));
    }

    @Tool(name = "replace_file_in_or_output_stream_finalize_with_close", description = "Replace invocations of `finalize()` on `FileInputStream` and `FileOutputStream` with `close()`. Replace invocations of the deprecated `finalize()` method on `FileInputStream` and `FileOutputStream` with `close()`.")
    public ToolResponse executeReplaceFileInOrOutputStreamFinalizeWithCloseRecipe() throws IOException {
        log.info("Execute ReplaceFileInOrOutputStreamFinalizeWithClose Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(ReplaceFileInOrOutputStreamFinalizeWithClose.class));
    }

    @Tool(name = "application_path_wildcard_no_longer_accepted", description = "Remove trailing `/*` from `jakarta.ws.rs.ApplicationPath` values.")
    public ToolResponse executeApplicationPathWildcardNoLongerAcceptedRecipe() throws IOException {
        log.info("Execute ApplicationPathWildcardNoLongerAccepted Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(ApplicationPathWildcardNoLongerAccepted.class));
    }

    @Tool(name = "remove_bean_is_nullable", description = "Remove `Bean.isNullable()`. `Bean.isNullable()` has been removed in CDI 4.0.0, and now always returns `false`.")
    public ToolResponse executeRemoveBeanIsNullableRecipe() throws IOException {
        log.info("Execute RemoveBeanIsNullable Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(RemoveBeanIsNullable.class));
    }

    @Tool(name = "update_bean_manager_methods", description = "Update `fireEvent()` and `createInjectionTarget()` calls.  Updates `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()`.")
    public ToolResponse executeUpdateBeanManagerMethodsRecipe() throws IOException {
        log.info("Execute UpdateBeanManagerMethods Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(UpdateBeanManagerMethods.class));
    }

    @Tool(name = "update_get_real_path", description = "Updates `getRealPath()` to call `getContext()` followed by `getRealPath()`. Updates `getRealPath()` for `jakarta.servlet.ServletRequest` and `jakarta.servlet.ServletRequestWrapper` to use `ServletContext.getRealPath(String)`.")
    public ToolResponse executeUpdateGetRealPathRecipe() throws IOException {
        log.info("Execute UpdateGetRealPath Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(UpdateGetRealPath.class));
    }

    @Tool(name = "add_column_annotation", description = "`@ElementCollection` annotations must be accompanied by a defined `@Column` annotation. When an attribute is annotated with `@ElementCollection`, a separate table is created for the attribute that includes the attribute \nID and value. In OpenJPA, the column for the annotated attribute is named element, whereas EclipseLink names the column based on \nthe name of the attribute. To remain compatible with tables that were created with OpenJPA, add a `@Column` annotation with the name \nattribute set to element.")
    public ToolResponse executeAddColumnAnnotationRecipe() throws IOException {
        log.info("Execute AddColumnAnnotation Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(AddColumnAnnotation.class));
    }

    @Tool(name = "url_constructor_to_uri_create", description = "Converts `new URL(String)` constructor to `URI.create(String).toURL()`.")
    public ToolResponse executeURLConstructorToURICreateRecipe() throws IOException {
        log.info("Execute URLConstructorToURICreate Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(URLConstructorToURICreate.class));
    }

    @Tool(name = "add_default_constructor_to_entity_class", description = "`@Entity` objects with constructors must also have a default constructor. When a Java Persistence API (JPA) entity class has a constructor with arguments, the class must also have a default, no-argument constructor. The OpenJPA implementation automatically generates the no-argument constructor, but the EclipseLink implementation does not.")
    public ToolResponse executeAddDefaultConstructorToEntityClassRecipe() throws IOException {
        log.info("Execute AddDefaultConstructorToEntityClass Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(AddDefaultConstructorToEntityClass.class));
    }

    @Tool(name = "add_jaxws_runtime", description = "Use the latest JAX-WS API and runtime for Jakarta EE 8. Update build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility with Java version 11 or greater. The recipe will add a JAX-WS run-time, in Gradle `compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive dependency on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite the move to the Jakarta artifact**.")
    public ToolResponse executeAddJaxwsRuntimeRecipe() throws IOException {
        log.info("Execute AddJaxwsRuntime Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(AddJaxwsRuntime.class));
    }

    @Tool(name = "remove_temporal_annotation", description = "Remove the `@Temporal` annotation for some `java.sql` attributes. OpenJPA persists the fields of attributes of type `java.sql.Date`, `java.sql.Time`, or `java.sql.Timestamp` that have a `javax.persistence.Temporal` annotation, whereas EclipseLink throws an exception. Remove the `@Temporal` annotation so the behavior in EclipseLink will match the behavior in OpenJPA.")
    public ToolResponse executeRemoveTemporalAnnotationRecipe() throws IOException {
        log.info("Execute RemoveTemporalAnnotation Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(RemoveTemporalAnnotation.class));
    }

    @Tool(name = "string_formatted", description = "Prefer `String.formatted(Object...)` over `String.format(String, Object...)` in Java 17 or higher.")
    public ToolResponse executeStringFormattedRecipe() throws IOException {
        log.info("Execute StringFormatted Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(StringFormatted.class));
    }

    @Tool(name = "use_string_is_empty_recipe", description = "Replace `0 < s.length()` and `s.length() != 0` with `!s.isEmpty()`.")
    public ToolResponse executeUseStringIsEmptyRecipeRecipe() throws IOException {
        log.info("Execute UseStringIsEmptyRecipe Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(UseStringIsEmptyRecipe.class));
    }

    @Tool(name = "migrate_logger_global_to_get_global", description = "Use `Logger#getGlobal()`. The preferred way to get the global logger object is via the call `Logger#getGlobal()` over direct field access to `java.util.logging.Logger.global`.")
    public ToolResponse executeMigrateLoggerGlobalToGetGlobalRecipe() throws IOException {
        log.info("Execute MigrateLoggerGlobalToGetGlobal Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateLoggerGlobalToGetGlobal.class));
    }

    @Tool(name = "migrate_log_record_set_millis_to_set_instant", description = "Use `LogRecord#setInstant(Instant)` instead of the deprecated `LogRecord#setMillis(long)` in Java 9 or higher.")
    public ToolResponse executeMigrateLogRecordSetMillisToSetInstantRecipe() throws IOException {
        log.info("Execute MigrateLogRecordSetMillisToSetInstant Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateLogRecordSetMillisToSetInstant.class));
    }

    @Tool(name = "migrate_url_decoder_decode", description = "Use `java.net.URLDecoder#decode(String, StandardCharsets.UTF_8)` instead of the deprecated `java.net.URLDecoder#decode(String)` in Java 10 or higher.")
    public ToolResponse executeMigrateURLDecoderDecodeRecipe() throws IOException {
        log.info("Execute MigrateURLDecoderDecode Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateURLDecoderDecode.class));
    }

    @Tool(name = "migrate_url_encoder_encode", description = "Use `java.net.URLEncoder#encode(String, StandardCharsets.UTF_8)` instead of the deprecated `java.net.URLEncoder#encode(String)` in Java 10 or higher.")
    public ToolResponse executeMigrateURLEncoderEncodeRecipe() throws IOException {
        log.info("Execute MigrateURLEncoderEncode Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateURLEncoderEncode.class));
    }

    @Tool(name = "url_constructors_to_new_uri", description = "Converts `new URL(String, ..)` constructors to `new URI(String, ..).toURL()`.")
    public ToolResponse executeURLConstructorsToNewURIRecipe() throws IOException {
        log.info("Execute URLConstructorsToNewURI Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(URLConstructorsToNewURI.class));
    }

    @Tool(name = "migrate_driver_manager_set_log_stream", description = "Use `DriverManager#setLogWriter(java.io.PrintWriter)` instead of the deprecated `DriverManager#setLogStream(java.io.PrintStream)` in Java 1.2 or higher.")
    public ToolResponse executeMigrateDriverManagerSetLogStreamRecipe() throws IOException {
        log.info("Execute MigrateDriverManagerSetLogStream Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateDriverManagerSetLogStream.class));
    }

    @Tool(name = "iterator_next", description = "Replace `iterator().next()` with `getFirst()`. Replace `SequencedCollection.iterator().next()` with `getFirst()`.")
    public ToolResponse executeIteratorNextRecipe() throws IOException {
        log.info("Execute IteratorNext Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(IteratorNext.class));
    }

    @Tool(name = "list_first_and_last", description = "Replace `List.get(int)`, `add(int, Object)`, and `remove(int)` with `SequencedCollection` `*First` and `*Last` methods. Replace `list.get(0)` with `list.getFirst()`, `list.get(list.size() - 1)` with `list.getLast()`, and similar for `add(int, E)` and `remove(int)`.")
    public ToolResponse executeListFirstAndLastRecipe() throws IOException {
        log.info("Execute ListFirstAndLast Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(ListFirstAndLast.class));
    }

    @Tool(name = "migrate_collections_singleton_list", description = "Prefer `List.of(..)` instead of using `Collections.singletonList()` in Java 9 or higher.")
    public ToolResponse executeMigrateCollectionsSingletonListRecipe() throws IOException {
        log.info("Execute MigrateCollectionsSingletonList Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateCollectionsSingletonList.class));
    }

    @Tool(name = "migrate_collections_singleton_map", description = "Prefer `Map.Of(..)` instead of using `Collections.singletonMap()` in Java 9 or higher.")
    public ToolResponse executeMigrateCollectionsSingletonMapRecipe() throws IOException {
        log.info("Execute MigrateCollectionsSingletonMap Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateCollectionsSingletonMap.class));
    }

    @Tool(name = "migrate_collections_unmodifiable_list", description = "Prefer `List.Of(..)` instead of using `unmodifiableList(java.util.Arrays asList(<args>))` in Java 9 or higher.")
    public ToolResponse executeMigrateCollectionsUnmodifiableListRecipe() throws IOException {
        log.info("Execute MigrateCollectionsUnmodifiableList Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(MigrateCollectionsUnmodifiableList.class));
    }

    @Tool(name = "use_locale_of", description = "Prefer `Locale.of(..)` over `new Locale(..)` in Java 19 or higher.")
    public ToolResponse executeUseLocaleOfRecipe() throws IOException {
        log.info("Execute UseLocaleOf Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(UseLocaleOf.class));
    }

    @Tool(name = "use_enum_set_of", description = "Prefer `EnumSet of(..)` instead of using `Set of(..)` when the arguments are enums in Java 5 or higher.")
    public ToolResponse executeUseEnumSetOfRecipe() throws IOException {
        log.info("Execute UseEnumSetOf Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(UseEnumSetOf.class));
    }

    @Tool(name = "use_map_of", description = "Prefer `Map.of(..)` instead of using `java.util.Map#put(..)` in Java 10 or higher.")
    public ToolResponse executeUseMapOfRecipe() throws IOException {
        log.info("Execute UseMapOf Recipe");
        return executeRecipe(RecipeIntrospectionUtils.constructRecipe(UseMapOf.class));
    }

    @Tool(name = "list_all_available_java_migration_tools", description = "Lists of the available Java migration tools.")
    public ToolResponse listAllTheAvailableJavaMigrationTools() throws JsonProcessingException {
        log.info("List All The Available Java Migration Tools");
        return ToolResponse.success(getRecipeAsJson());
    }

    private static ToolResponse executeRecipe(Recipe recipe) throws IOException {
        // Create execution context
        ExecutionContext executionContext = new InMemoryExecutionContext(t -> t.printStackTrace());

        // Create Java parser
        JavaParser javaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build();

        // Parse the Java files
        List<SourceFile> sourceFiles = javaParser.parse(JAVA_FILES, ROOT_PATH, executionContext).collect(Collectors.toList());

        // Apply the recipe
        RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(sourceFiles).generate(sourceFiles), executionContext);

        // Process results
        List<Result> results = recipeRun.getChangeset().getAllResults();
        for (Result result : results) {
            // Write the changes back to disk
            Path absolutePath = ROOT_PATH.resolve(result.getBefore().getSourcePath());
            Files.writeString(absolutePath, result.getAfter().printAll());
        }

        if (results.isEmpty()) {
            return ToolResponse.success("Executing the recipe " + recipe.getDisplayName() + " made no change in the code located in " + ROOT);
        } else {
            return ToolResponse.success("Executing the recipe " + recipe.getDisplayName() + " made " + results.size() + " changes in the code located in " + ROOT);
        }
    }

    String getRecipeAsJson() throws JsonProcessingException {

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

