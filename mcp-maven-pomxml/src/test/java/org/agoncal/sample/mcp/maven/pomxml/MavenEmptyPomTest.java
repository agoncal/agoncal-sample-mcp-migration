package org.agoncal.sample.mcp.maven.pomxml;

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
        List<DependencyRecord> depMgmt = service.getAllDependencyManagementDependencies();

        assertNotNull(depMgmt);
        assertTrue(depMgmt.isEmpty());
        assertEquals(0, depMgmt.size());
    }
}
