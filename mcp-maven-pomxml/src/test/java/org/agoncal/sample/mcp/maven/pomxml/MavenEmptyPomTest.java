package org.agoncal.sample.mcp.maven.pomxml;

import static io.smallrye.common.constraint.Assert.assertFalse;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

class MavenEmptyPomTest {

    private MavenDependencyService service;

    // Custom service that points to the empty POM
    private static class EmptyPomService extends MavenDependencyService {
        private static final String EMPTY_POM_PATH = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration/mcp-maven-pomxml/src/test/resources/pomempty.xml";

        @Override
        protected Path getPomPath() {
            return Paths.get(EMPTY_POM_PATH).toAbsolutePath();
        }
    }

    @BeforeEach
    void setUp() {
        service = new EmptyPomService();
    }

    @Test
    void testEmptyPomHasNoProperties() throws IOException, XmlPullParserException {
        List<PropertyRecord> properties = service.getAllProperties();

        assertNotNull(properties);
        assertTrue(properties.isEmpty());
        assertEquals(0, properties.size());
    }

    @Test
    void testEmptyPomHasNoDependencies() throws IOException, XmlPullParserException {
        List<DependencyRecord> dependencies = service.getAllDependencies();

        assertNotNull(dependencies);
        assertTrue(dependencies.isEmpty());
        assertEquals(0, dependencies.size());
    }

    @Test
    void testEmptyPomHasNoPlugins() throws IOException, XmlPullParserException {
        List<PluginRecord> plugins = service.getAllPlugins();

        assertNotNull(plugins);
        assertTrue(plugins.isEmpty());
        assertEquals(0, plugins.size());
    }

    @Test
    void testEmptyPomHasNoDependencyManagement() throws IOException, XmlPullParserException {
        List<DependencyRecord> depMgmt = service.getAllDependenciesInDependencyManagements();

        assertNotNull(depMgmt);
        assertTrue(depMgmt.isEmpty());
        assertEquals(0, depMgmt.size());
    }

    @Test
    void testAddAndRemoveDependency() throws IOException, XmlPullParserException {
        // Test with main POM dependency - bootstrap
        String mainGroupId = "org.webjars";
        String mainArtifactId = "bootstrap";
        String mainVersion = "${version.bootstrap}";
        String mainType = null;
        String mainScope = null; // compile is default

        // Verify dependency does not exist initially
        assertFalse(service.dependencyExists(null, mainGroupId, mainArtifactId));

        // Add main POM dependency
        service.addNewDependency(null, mainGroupId, mainArtifactId, mainVersion, mainType, mainScope);
        assertTrue(service.dependencyExists(null, mainGroupId, mainArtifactId));

        // Remove main POM dependency
        service.removeExistingDependency(null, mainGroupId, mainArtifactId);
        assertFalse(service.dependencyExists(null, mainGroupId, mainArtifactId));

    }

    @Test
    void testAddAndRemoveProperty() throws IOException, XmlPullParserException {
        // Test with main POM property - version.bootstrap
        String mainPropertyKey = "version.bootstrap";
        String mainPropertyValue = "5.3.0";

        // Verify property does not exist initially
        List<PropertyRecord> properties = service.getAllProperties();
        Optional<PropertyRecord> originalMainProp = properties.stream()
            .filter(prop -> prop.profile() == null && mainPropertyKey.equals(prop.key()))
            .findFirst();
        assertFalse(originalMainProp.isPresent());

        // Add main POM property
        service.addNewProperty(null, mainPropertyKey, mainPropertyValue);
        properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop ->
            prop.profile() == null &&
                mainPropertyKey.equals(prop.key()) &&
                mainPropertyValue.equals(prop.value())));

        // Remove main POM property
        service.removeExistingProperty(null, mainPropertyKey);
        properties = service.getAllProperties();
        assertFalse(properties.stream().anyMatch(prop ->
            prop.profile() == null && mainPropertyKey.equals(prop.key())));

    }

    @Test
    void testAddAndRemovePlugin() throws IOException, XmlPullParserException {
        // Test with main POM plugin - maven-surefire-plugin
        String mainGroupId = null; // maven group (defaults to org.apache.maven.plugins)
        String mainArtifactId = "maven-surefire-plugin";
        String mainVersion = "${version.maven.surefire.plugin}";
        Boolean mainInherited = null; // not specified in XML

        // Verify plugin does not exists initially
        List<PluginRecord> plugins = service.getAllPlugins();
        Optional<PluginRecord> originalMainPlugin = plugins.stream()
            .filter(plugin -> plugin.profile() == null && mainArtifactId.equals(plugin.artifactId()))
            .findFirst();
        assertFalse(originalMainPlugin.isPresent());

        // Add main POM plugin
        service.addNewPlugin(null, mainGroupId, mainArtifactId, mainVersion, mainInherited);
        plugins = service.getAllPlugins();
        assertTrue(plugins.stream().anyMatch(plugin ->
            plugin.profile() == null && mainArtifactId.equals(plugin.artifactId())));

        // Remove main POM plugin
        service.removeExistingPlugin(null, mainGroupId, mainArtifactId);
        plugins = service.getAllPlugins();
        assertFalse(plugins.stream().anyMatch(plugin ->
            plugin.profile() == null && mainArtifactId.equals(plugin.artifactId())));
    }

}
