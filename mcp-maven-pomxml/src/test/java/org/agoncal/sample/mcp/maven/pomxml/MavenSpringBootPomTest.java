package org.agoncal.sample.mcp.maven.pomxml;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;
import org.agoncal.sample.mcp.maven.pomxml.model.DependencyRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.ParentRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.PluginRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.PropertyRecord;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class MavenSpringBootPomTest {

    private MavenDependencyService service;

    // Custom service that points to the SpringBoot POM
    private static class JHipsterPomService extends MavenDependencyService {
        private static final String EMPTY_POM_PATH = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration/mcp-maven-pomxml/src/test/resources/pomspringboot.xml";

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
    void testSpringBootPomHasProperties() throws IOException, XmlPullParserException {
        List<PropertyRecord> properties = service.getAllProperties();

        assertNotNull(properties);
        assertFalse(properties.isEmpty());
        assertEquals(1, properties.size());
    }

    @Test
    void testSpringBootPomHasDependencies() throws IOException, XmlPullParserException {
        List<DependencyRecord> dependencies = service.getAllDependencies();

        assertNotNull(dependencies);
        assertFalse(dependencies.isEmpty());
        assertEquals(2, dependencies.size());
    }

    @Test
    void testSpringBootPomHasPlugins() throws IOException, XmlPullParserException {
        List<PluginRecord> plugins = service.getAllPlugins();

        assertNotNull(plugins);
        assertFalse(plugins.isEmpty());
        assertEquals(1, plugins.size());
    }

    @Test
    void testSpringBootPomHasDependencyManagement() throws IOException, XmlPullParserException {
        List<DependencyRecord> depMgmt = service.getAllDependenciesInDependencyManagements();

        assertNotNull(depMgmt);
        assertTrue(depMgmt.isEmpty());
        assertEquals(0, depMgmt.size());
    }

    @Test
    void testGetParent() throws IOException, XmlPullParserException {
        ParentRecord parent = service.getParent();

        assertNotNull(parent);
        assertEquals("org.springframework.boot", parent.groupId());
        assertEquals("spring-boot-starter-parent", parent.artifactId());
        assertEquals("3.5.4", parent.version());
        assertEquals("", parent.relativePath()); // relativePath is empty in pomspringboot.xml
    }

    @Test
    void testUpdateParentVersion() throws IOException, XmlPullParserException {
        // Get the parent first to verify it exists and get the original version
        ParentRecord originalParent = service.getParent();
        assertNotNull(originalParent);
        String originalVersion = originalParent.version();
        assertEquals("3.5.4", originalVersion); // Verify expected original version
        
        String newVersion = "3.2.1";

        // Update parent version to new version
        service.updateParentVersion(newVersion);

        // Verify the version was updated by getting the parent again
        ParentRecord updatedParent = service.getParent();
        assertNotNull(updatedParent);
        assertEquals(newVersion, updatedParent.version());
        
        // Verify other parent fields remain unchanged
        assertEquals(originalParent.groupId(), updatedParent.groupId());
        assertEquals(originalParent.artifactId(), updatedParent.artifactId());
        assertEquals(originalParent.relativePath(), updatedParent.relativePath());

        // Restore original version
        service.updateParentVersion(originalVersion);
        
        // Verify the version was restored correctly
        ParentRecord restoredParent = service.getParent();
        assertNotNull(restoredParent);
        assertEquals(originalVersion, restoredParent.version());
        
        // Verify other parent fields remain unchanged
        assertEquals(originalParent.groupId(), restoredParent.groupId());
        assertEquals(originalParent.artifactId(), restoredParent.artifactId());
        assertEquals(originalParent.relativePath(), restoredParent.relativePath());
    }
}
