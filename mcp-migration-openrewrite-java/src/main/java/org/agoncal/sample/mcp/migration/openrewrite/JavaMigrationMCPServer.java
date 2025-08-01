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
    private static final String ROOT_APP_TO_MIGRATE = System.getenv("ROOT_APP_TO_MIGRATE");
    private static final Path ROOT_PATH = Paths.get(ROOT_APP_TO_MIGRATE);
    private static final File ROOT_DIRECTORY = Paths.get(ROOT_APP_TO_MIGRATE).toFile();
    private static ExecutionContext executionContext;
    private static List<SourceFile> sourceFiles;

    @PostConstruct
    void findJavaFiles() {
        log.info("Finding the number of Java files in the directory: " + ROOT_APP_TO_MIGRATE);
        List<Path> javaFiles = new ArrayList<>();
        if (ROOT_DIRECTORY.exists()) {
            collectJavaFiles(ROOT_DIRECTORY, javaFiles);
        } else {
            System.err.println("Directory does not exist: " + ROOT_DIRECTORY);
        }
        log.info("Found " + javaFiles.size() + " Java files in the directory: " + ROOT_DIRECTORY);

        // Create execution context
        executionContext = new InMemoryExecutionContext(t -> t.printStackTrace());

        // Forces Java version so we can pick up a different parser
        //System.setProperty("java.version", "11.0.2");
        log.info("Java Version " + System.getProperty("java.version"));

        // Create Java parser
        JavaParser javaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build();

        // Parse the Java files
        sourceFiles = javaParser.parse(javaFiles, ROOT_PATH, executionContext).collect(Collectors.toList());
        log.info("Parsed " + sourceFiles.size() + " Java files in the root path: " + ROOT_PATH);
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

    @Tool(name = "list_all_available_java_migration_tools", description = "Lists of the available Java migration tools.")
    public ToolResponse listAllTheAvailableJavaMigrationTools() throws JsonProcessingException {
        log.info("List all the " + recipesToExpose.size() + " available Java Migration Tools");
        return ToolResponse.success(getRecipeAsJson());
    }

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

    @Tool(name = "change_default_key_store", description = "Return String `jks` when  `KeyStore.getDefaultType()` is called. In Java 11 the default keystore was updated from JKS to PKCS12. As a result, applications relying on KeyStore.getDefaultType() may encounter issues after migrating, unless their JKS keystore has been converted to PKCS12. This returns default key store of `jks` when `KeyStore.getDefaultType()` method is called to use the pre Java 11 default keystore.")
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

    @Tool(name = "add_jaxws_runtime", description = "Use the latest JAX-WS API and runtime for Jakarta EE 8. Update build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility with Java version 11 or greater. The will add a JAX-WS run-time, in Gradle `compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive dependency on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite the move to the Jakarta artifact**.")
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
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.BeansXmlNamespace",
                "name": "beans_xml_namespace",
                "displayName": "Change `beans.xml` `schemaLocation` to match XML namespace",
                "description": "Set the `schemaLocation` that corresponds to the `xmlns` set in `beans.xml` files.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.CastArraysAsListToList",
                "name": "cast_arrays_as_list_to_list",
                "displayName": "Remove explicit casts on `Arrays.asList(..).toArray()`",
                "description": "Convert code like `(Integer[]) Arrays.asList(1, 2, 3).toArray()` to `Arrays.asList(1, 2, 3).toArray(new Integer[0])`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.ChangeDefaultKeyStore",
                "name": "change_default_key_store",
                "displayName": "Return String `jks` when  `KeyStore.getDefaultType()` is called",
                "description": "In Java 11 the default keystore was updated from JKS to PKCS12. As a result, applications relying on KeyStore.getDefaultType() may encounter issues after migrating, unless their JKS keystore has been converted to PKCS12. This tool returns default key store of `jks` when `KeyStore.getDefaultType()` method is called to use the pre Java 11 default keystore.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.IllegalArgumentExceptionToAlreadyConnectedException",
                "name": "illegal_argument_exception_to_already_connected_exception",
                "displayName": "Replace `IllegalArgumentException` with `AlreadyConnectedException` in `DatagramChannel.send()` method",
                "description": "Replace `IllegalArgumentException` with `AlreadyConnectedException` for DatagramChannel.send() to ensure compatibility with Java 11+.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.JREThrowableFinalMethods",
                "name": "j_r_e_throwable_final_methods",
                "displayName": "Rename final method declarations `getSuppressed()` and `addSuppressed(Throwable exception)` in classes that extend `Throwable`",
                "description": "The tool renames  `getSuppressed()` and `addSuppressed(Throwable exception)` methods  in classes that extend `java.lang.Throwable` to `myGetSuppressed` and `myAddSuppressed(Throwable)`.These methods were added to Throwable in Java 7 and are marked final which cannot be overridden.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.RemovedSecurityManagerMethods",
                "name": "removed_security_manager_methods",
                "displayName": "Replace deprecated methods in`SecurityManager`",
                "description": "Replace `SecurityManager` methods `checkAwtEventQueueAccess()`, `checkSystemClipboardAccess()`, `checkMemberAccess()` and `checkTopLevelWindow()` deprecated in Java SE 11 by `checkPermission(new java.security.AllPermission())`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.ReplaceComSunAWTUtilitiesMethods",
                "name": "replace_com_sun_a_w_t_utilities_methods",
                "displayName": "Replace `com.sun.awt.AWTUtilities` static method invocations",
                "description": "This tool replaces several static calls  in `com.sun.awt.AWTUtilities` with the JavaSE 11 equivalent. The methods replaced are `AWTUtilities.isTranslucencySupported()`, `AWTUtilities.setWindowOpacity()`, `AWTUtilities.getWindowOpacity()`, `AWTUtilities.getWindowShape()`, `AWTUtilities.isWindowOpaque()`, `AWTUtilities.isTranslucencyCapable()` and `AWTUtilities.setComponentMixingCutoutShape()`.",
                "options": [
                  {
                    "name": "get_a_w_t_is_windows_translucency_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  },
                  {
                    "name": "is_window_opaque_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  },
                  {
                    "name": "is_translucency_capable_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  },
                  {
                    "name": "set_window_opacity_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  },
                  {
                    "name": "get_window_opacity_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  },
                  {
                    "name": "get_window_shape_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  },
                  {
                    "name": "set_component_mixing_cutout_shape_pattern",
                    "displayName": "Method pattern to replace",
                    "description": "The method pattern to match and replace.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.UpgradeJavaVersion",
                "name": "upgrade_java_version",
                "displayName": "Upgrade Java version",
                "description": "Upgrade build plugin configuration to use the specified Java version. This tool changes `java.toolchain.languageVersion` in `build.gradle(.kts)` of gradle projects, or maven-compiler-plugin target version and related settings. Will not downgrade if the version is newer than the specified version.",
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
                "fqn": "org.openrewrite.java.migrate.UseJavaUtilBase64",
                "name": "use_java_util_base64",
                "displayName": "Prefer `java.util.Base64` instead of `sun.misc`",
                "description": "Prefer `java.util.Base64` instead of using `sun.misc` in Java 8 or higher. `sun.misc` is not exported by the Java module system and accessing this class will result in a warning in Java 11 and an error in Java 17.",
                "options": [
                  {
                    "name": "use_mime_coder",
                    "displayName": "Use Mime Coder",
                    "description": "Use `Base64.getMimeEncoder()/getMimeDecoder()` instead of `Base64.getEncoder()/getDecoder()`.",
                    "type": "boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.lang.ThreadStopUnsupported",
                "name": "thread_stop_unsupported",
                "displayName": "Replace `Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` with `throw new UnsupportedOperationException()`",
                "description": "`Thread.resume()`, `Thread.stop()`, and `Thread.suspend()` always throws a `new UnsupportedOperationException` in Java 21+. This tool makes that explicit, as the migration is more complicated.See https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html .",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.io.ReplaceFileInOrOutputStreamFinalizeWithClose",
                "name": "replace_file_in_or_output_stream_finalize_with_close",
                "displayName": "Replace invocations of `finalize()` on `FileInputStream` and `FileOutputStream` with `close()`",
                "description": "Replace invocations of the deprecated `finalize()` method on `FileInputStream` and `FileOutputStream` with `close()`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.jakarta.ApplicationPathWildcardNoLongerAccepted",
                "name": "application_path_wildcard_no_longer_accepted",
                "displayName": "Remove trailing slash from `jakarta.ws.rs.ApplicationPath` values",
                "description": "Remove trailing `/*` from `jakarta.ws.rs.ApplicationPath` values.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.jakarta.RemoveBeanIsNullable",
                "name": "remove_bean_is_nullable",
                "displayName": "Remove `Bean.isNullable()`",
                "description": "`Bean.isNullable()` has been removed in CDI 4.0.0, and now always returns `false`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.jakarta.UpdateAnnotationAttributeJavaxToJakarta",
                "name": "update_annotation_attribute_javax_to_jakarta",
                "displayName": "Update annotation attributes using `javax` to `jakarta`",
                "description": "Replace `javax` with `jakarta` in annotation attributes for matching annotation signatures.",
                "options": [
                  {
                    "name": "signature",
                    "displayName": "Annotation signature",
                    "description": "An annotation signature to match.",
                    "type": "String"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.jakarta.UpdateBeanManagerMethods",
                "name": "update_bean_manager_methods",
                "displayName": "Update `fireEvent()` and `createInjectionTarget()` calls",
                "description": " Updates `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.jakarta.UpdateGetRealPath",
                "name": "update_get_real_path",
                "displayName": "Updates `getRealPath()` to call `getContext()` followed by `getRealPath()`",
                "description": "Updates `getRealPath()` for `jakarta.servlet.ServletRequest` and `jakarta.servlet.ServletRequestWrapper` to use `ServletContext.getRealPath(String)`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.javax.AddColumnAnnotation",
                "name": "add_column_annotation",
                "displayName": "`@ElementCollection` annotations must be accompanied by a defined `@Column` annotation",
                "description": "When an attribute is annotated with `@ElementCollection`, a separate table is created for the attribute that includes the attribute \\nID and value. In OpenJPA, the column for the annotated attribute is named element, whereas EclipseLink names the column based on \\nthe name of the attribute. To remain compatible with tables that were created with OpenJPA, add a `@Column` annotation with the name \\nattribute set to element.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.net.URLConstructorToURICreate",
                "name": "u_r_l_constructor_to_u_r_i_create",
                "displayName": "Convert `new URL(String)` to `URI.create(String).toURL()`",
                "description": "Converts `new URL(String)` constructor to `URI.create(String).toURL()`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.javax.AddDefaultConstructorToEntityClass",
                "name": "add_default_constructor_to_entity_class",
                "displayName": "`@Entity` objects with constructors must also have a default constructor",
                "description": "When a Java Persistence API (JPA) entity class has a constructor with arguments, the class must also have a default, no-argument constructor. The OpenJPA implementation automatically generates the no-argument constructor, but the EclipseLink implementation does not.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.javax.AddJaxwsRuntime",
                "name": "add_jaxws_runtime",
                "displayName": "Use the latest JAX-WS API and runtime for Jakarta EE 8",
                "description": "Update build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility with Java version 11 or greater. The tool will add a JAX-WS run-time, in Gradle `compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive dependency on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite the move to the Jakarta artifact**.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.javax.RemoveTemporalAnnotation",
                "name": "remove_temporal_annotation",
                "displayName": "Remove the `@Temporal` annotation for some `java.sql` attributes",
                "description": "OpenJPA persists the fields of attributes of type `java.sql.Date`, `java.sql.Time`, or `java.sql.Timestamp` that have a `javax.persistence.Temporal` annotation, whereas EclipseLink throws an exception. Remove the `@Temporal` annotation so the behavior in EclipseLink will match the behavior in OpenJPA.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.lang.StringFormatted",
                "name": "string_formatted",
                "displayName": "Prefer `String.formatted(Object...)`",
                "description": "Prefer `String.formatted(Object...)` over `String.format(String, Object...)` in Java 17 or higher.",
                "options": [
                  {
                    "name": "add_parentheses",
                    "displayName": "Add parentheses around the first argument",
                    "description": "Add parentheses around the first argument if it is not a simple expression. Default true; if false no change will be made. ",
                    "type": "Boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.lang.UseStringIsEmptyRecipe",
                "name": "use_string_is_empty",
                "displayName": "Replace `0 < s.length()` with `!s.isEmpty()`",
                "description": "Replace `0 < s.length()` and `s.length() != 0` with `!s.isEmpty()`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.lang.UseTextBlocks",
                "name": "use_text_blocks",
                "displayName": "Use text blocks",
                "description": "Text blocks are easier to read than concatenated strings.",
                "options": [
                  {
                    "name": "convert_strings_without_newlines",
                    "displayName": "Whether to convert strings without newlines (the default value is true).",
                    "description": "Whether or not strings without newlines should be converted to text block when processing code. The default value is true.",
                    "type": "boolean"
                  }
                ]
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.logging.MigrateLoggerGlobalToGetGlobal",
                "name": "migrate_logger_global_to_get_global",
                "displayName": "Use `Logger#getGlobal()`",
                "description": "The preferred way to get the global logger object is via the call `Logger#getGlobal()` over direct field access to `java.util.logging.Logger.global`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.logging.MigrateLogRecordSetMillisToSetInstant",
                "name": "migrate_log_record_set_millis_to_set_instant",
                "displayName": "Use `LogRecord#setInstant(Instant)`",
                "description": "Use `LogRecord#setInstant(Instant)` instead of the deprecated `LogRecord#setMillis(long)` in Java 9 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.net.MigrateURLDecoderDecode",
                "name": "migrate_u_r_l_decoder_decode",
                "displayName": "Use `java.net.URLDecoder#decode(String, StandardCharsets.UTF_8)`",
                "description": "Use `java.net.URLDecoder#decode(String, StandardCharsets.UTF_8)` instead of the deprecated `java.net.URLDecoder#decode(String)` in Java 10 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.net.MigrateURLEncoderEncode",
                "name": "migrate_u_r_l_encoder_encode",
                "displayName": "Use `java.net.URLEncoder#encode(String, StandardCharsets.UTF_8)`",
                "description": "Use `java.net.URLEncoder#encode(String, StandardCharsets.UTF_8)` instead of the deprecated `java.net.URLEncoder#encode(String)` in Java 10 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.net.URLConstructorsToNewURI",
                "name": "u_r_l_constructors_to_new_u_r_i",
                "displayName": "Convert `new URL(String, ..)` to `new URI(String, ..).toURL()`",
                "description": "Converts `new URL(String, ..)` constructors to `new URI(String, ..).toURL()`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.sql.MigrateDriverManagerSetLogStream",
                "name": "migrate_driver_manager_set_log_stream",
                "displayName": "Use `DriverManager#setLogWriter(java.io.PrintWriter)`",
                "description": "Use `DriverManager#setLogWriter(java.io.PrintWriter)` instead of the deprecated `DriverManager#setLogStream(java.io.PrintStream)` in Java 1.2 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.IteratorNext",
                "name": "iterator_next",
                "displayName": "Replace `iterator().next()` with `getFirst()`",
                "description": "Replace `SequencedCollection.iterator().next()` with `getFirst()`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.ListFirstAndLast",
                "name": "list_first_and_last",
                "displayName": "Replace `List.get(int)`, `add(int, Object)`, and `remove(int)` with `SequencedCollection` `*First` and `*Last` methods",
                "description": "Replace `list.get(0)` with `list.getFirst()`, `list.get(list.size() - 1)` with `list.getLast()`, and similar for `add(int, E)` and `remove(int)`.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.MigrateCollectionsSingletonList",
                "name": "migrate_collections_singleton_list",
                "displayName": "Prefer `List.of(..)`",
                "description": "Prefer `List.of(..)` instead of using `Collections.singletonList()` in Java 9 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.MigrateCollectionsSingletonMap",
                "name": "migrate_collections_singleton_map",
                "displayName": "Prefer `Map.of(..)`",
                "description": "Prefer `Map.Of(..)` instead of using `Collections.singletonMap()` in Java 9 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.MigrateCollectionsUnmodifiableList",
                "name": "migrate_collections_unmodifiable_list",
                "displayName": "Prefer `List.of(..)`",
                "description": "Prefer `List.Of(..)` instead of using `unmodifiableList(java.util.Arrays asList(<args>))` in Java 9 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.UseEnumSetOf",
                "name": "use_enum_set_of",
                "displayName": "Prefer `EnumSet of(..)`",
                "description": "Prefer `EnumSet of(..)` instead of using `Set of(..)` when the arguments are enums in Java 5 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.UseLocaleOf",
                "name": "use_locale_of",
                "displayName": "Prefer `Locale.of(..)` over `new Locale(..)`",
                "description": "Prefer `Locale.of(..)` over `new Locale(..)` in Java 19 or higher.",
                "options": []
              },
              {
                "migration": "Java Migration",
                "fqn": "org.openrewrite.java.migrate.util.UseMapOf",
                "name": "use_map_of",
                "displayName": "Prefer `Map.of(..)`",
                "description": "Prefer `Map.of(..)` instead of using `java.util.Map#put(..)` in Java 10 or higher.",
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

