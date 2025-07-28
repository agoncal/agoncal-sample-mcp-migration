package org.agoncal.sample.mcp.maven.pomxml;

import static io.smallrye.common.constraint.Assert.assertFalse;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class MavenJHipsterPomTest {

    private MavenDependencyService service;

    // Custom service that points to the JHipster POM
    private static class JHipsterPomService extends MavenDependencyService {
        private static final String EMPTY_POM_PATH = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration/mcp-maven-pomxml/src/test/resources/pomjhipster.xml";

        @Override
        protected Path getPomPath() {
            return Paths.get(EMPTY_POM_PATH).toAbsolutePath();
        }
    }

    @BeforeEach
    void setUp() {
        service = new JHipsterPomService();
    }

    @Test
    void testJHipsterPomHasProperties() throws IOException, XmlPullParserException {
        List<PropertyRecord> properties = service.getAllProperties();

        assertNotNull(properties);
        assertFalse(properties.isEmpty());
        assertEquals(65, properties.size());
    }

    @Test
    void testJHipsterPomHasDependencies() throws IOException, XmlPullParserException {
        List<DependencyRecord> dependencies = service.getAllDependencies();

        assertNotNull(dependencies);
        assertFalse(dependencies.isEmpty());
        assertEquals(62, dependencies.size());
    }

    @Test
    void testJHipsterPomHasPlugins() throws IOException, XmlPullParserException {
        List<PluginRecord> plugins = service.getAllPlugins();

        assertNotNull(plugins);
        assertFalse(plugins.isEmpty());
        assertEquals(27, plugins.size());
    }

    @Test
    void testJHipsterPomHasDependencyManagement() throws IOException, XmlPullParserException {
        List<DependencyRecord> depMgmt = service.getAllDependenciesInDependencyManagements();

        assertNotNull(depMgmt);
        assertFalse(depMgmt.isEmpty());
        assertEquals(1, depMgmt.size());
    }
}
