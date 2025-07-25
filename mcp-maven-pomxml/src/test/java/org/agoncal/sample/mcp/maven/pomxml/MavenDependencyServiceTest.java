package org.agoncal.sample.mcp.maven.pomxml;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MavenDependencyServiceTest {

    private MavenDependencyService service;

    @BeforeEach
    void setUp() {
        service = new MavenDependencyService();
    }

    @Test
    void testGetAllDependencies() throws IOException, XmlPullParserException {
        List<DependencyRecord> dependencies = service.getAllDependencies();
        
        assertNotNull(dependencies);
        assertFalse(dependencies.isEmpty());
        
        // Just verify we get some dependencies and they have the right structure
        assertTrue(dependencies.size() >= 4); // At least original dependencies
        
        // Check we have both main and profile dependencies
        boolean hasMainDependencies = dependencies.stream()
            .anyMatch(dep -> dep.profile() == null);
        assertTrue(hasMainDependencies);
        
        // Verify specific main dependencies that should always exist
        assertTrue(dependencies.stream().anyMatch(dep -> 
            dep.profile() == null && 
            "jakarta.platform".equals(dep.groupId()) && 
            "jakarta.jakartaee-api".equals(dep.artifactId()) &&
            "provided".equals(dep.scope())));
            
        assertTrue(dependencies.stream().anyMatch(dep -> 
            dep.profile() == null && 
            "org.junit.jupiter".equals(dep.groupId()) && 
            "junit-jupiter".equals(dep.artifactId()) &&
            "test".equals(dep.scope())));
    }

    @Test
    void testGetAllDependencyManagementDependencies() throws IOException, XmlPullParserException {
        List<DependencyRecord> depMgmt = service.getAllDependencyManagementDependencies();
        
        assertNotNull(depMgmt);
        assertTrue(depMgmt.size() >= 2); // At least original dependencies
        
        // Main dependencyManagement
        assertTrue(depMgmt.stream().anyMatch(dep -> 
            dep.profile() == null && 
            "org.jboss.arquillian".equals(dep.groupId()) && 
            "arquillian-bom".equals(dep.artifactId()) &&
            "pom".equals(dep.type()) &&
            "import".equals(dep.scope())));
        
        // Profile dependencyManagement
        assertTrue(depMgmt.stream().anyMatch(dep -> 
            "jakarta-ee".equals(dep.profile()) && 
            "org.jboss.arquillian".equals(dep.groupId()) && 
            "arquillian-bom".equals(dep.artifactId()) &&
            "pom".equals(dep.type()) &&
            "import".equals(dep.scope())));
    }

    @Test
    void testGetAllProperties() throws IOException, XmlPullParserException {
        List<PropertyRecord> properties = service.getAllProperties();
        
        assertNotNull(properties);
        assertFalse(properties.isEmpty());
        
        // Check we have both main and profile properties
        boolean hasMainProperties = properties.stream()
            .anyMatch(prop -> prop.profile() == null);
        assertTrue(hasMainProperties);
        
        boolean hasProfileProperties = properties.stream()
            .anyMatch(prop -> prop.profile() != null);
        assertTrue(hasProfileProperties);
        
        // Verify specific main properties that should always exist
        assertTrue(properties.stream().anyMatch(prop -> 
            prop.profile() == null && 
            "version.junit".equals(prop.key())));
            
        // Verify we have properties from jakarta-ee profile
        assertTrue(properties.stream().anyMatch(prop -> 
            "jakarta-ee".equals(prop.profile()) && 
            "version.jakarta.ee".equals(prop.key())));
        
        // Verify we have properties from jacoco profile
        assertTrue(properties.stream().anyMatch(prop -> 
            "jacoco".equals(prop.profile())));
    }

    @Test
    void testGetAllProfiles() throws IOException, XmlPullParserException {
        List<ProfileRecord> profiles = service.getAllProfiles();
        
        assertNotNull(profiles);
        assertEquals(2, profiles.size());
        
        assertTrue(profiles.stream().anyMatch(profile -> 
            "jakarta-ee".equals(profile.id())));
        assertTrue(profiles.stream().anyMatch(profile -> 
            "jacoco".equals(profile.id())));
    }

    @Test
    void testGetAllPlugins() throws IOException, XmlPullParserException {
        List<PluginRecord> plugins = service.getAllPlugins();
        
        assertNotNull(plugins);
        assertFalse(plugins.isEmpty());
        
        // Main build has 5 plugins + jacoco profile has 2 plugins = 7 total (after modifications)
        assertEquals(7, plugins.size());
        
        // Check main plugins
        long mainPlugins = plugins.stream()
            .filter(plugin -> plugin.profile() == null)
            .count();
        assertEquals(5, mainPlugins);
        
        // Check profile plugins
        long profilePlugins = plugins.stream()
            .filter(plugin -> "jacoco".equals(plugin.profile()))
            .count();
        assertEquals(2, profilePlugins);
        
        // Verify specific main plugin
        assertTrue(plugins.stream().anyMatch(plugin -> 
            plugin.profile() == null && 
            "maven-compiler-plugin".equals(plugin.artifactId()) &&
            "true".equals(plugin.inherited())));
        
        // Verify plugin with dependencies (maven-compiler-plugin has 2 dependencies)
        Optional<PluginRecord> compilerPlugin = plugins.stream()
            .filter(plugin -> "maven-compiler-plugin".equals(plugin.artifactId()))
            .findFirst();
        assertTrue(compilerPlugin.isPresent());
        assertEquals(2, compilerPlugin.get().dependencies().size());
        
        // Verify profile plugin
        assertTrue(plugins.stream().anyMatch(plugin -> 
            "jacoco".equals(plugin.profile()) && 
            "org.jacoco".equals(plugin.groupId()) &&
            "jacoco-maven-plugin".equals(plugin.artifactId())));
    }

    @Test
    void testGetAllDependenciesByScope() throws IOException, XmlPullParserException {
        // Test 'test' scope dependencies
        List<DependencyRecord> testDeps = service.getAllDependenciesByScope("test");
        assertNotNull(testDeps);
        assertEquals(3, testDeps.size()); // mockito, junit, arquillian (derby was removed/replaced)
        
        assertTrue(testDeps.stream().allMatch(dep -> "test".equals(dep.scope())));
        assertTrue(testDeps.stream().anyMatch(dep -> 
            "org.mockito".equals(dep.groupId()) && 
            "mockito-core".equals(dep.artifactId())));
        assertTrue(testDeps.stream().anyMatch(dep -> 
            "jakarta-ee".equals(dep.profile()) && 
            "test.profile".equals(dep.groupId())));
        
        // Test 'provided' scope dependencies
        List<DependencyRecord> providedDeps = service.getAllDependenciesByScope("provided");
        assertNotNull(providedDeps);
        assertEquals(1, providedDeps.size());
        assertTrue(providedDeps.stream().anyMatch(dep -> 
            "jakarta.platform".equals(dep.groupId()) && 
            "jakarta.jakartaee-api".equals(dep.artifactId())));
        
        // Test 'compile' scope dependencies (default scope)
        List<DependencyRecord> compileDeps = service.getAllDependenciesByScope("compile");
        assertNotNull(compileDeps);
        assertEquals(2, compileDeps.size()); // jquery, test-artifact
        assertTrue(compileDeps.stream().allMatch(dep -> 
            dep.scope() == null || "compile".equals(dep.scope())));
    }

    @Test
    void testDependencyExists() throws IOException, XmlPullParserException {
        // Test main POM dependency
        assertTrue(service.dependencyExists(null, "jakarta.platform", "jakarta.jakartaee-api"));
        assertTrue(service.dependencyExists("jakarta.platform", "jakarta.jakartaee-api"));
        
        // Test profile dependency (updated to test.profile)
        assertTrue(service.dependencyExists("jakarta-ee", "test.profile", "profile-artifact"));
        
        // Test non-existent dependency
        assertFalse(service.dependencyExists(null, "non.existent", "artifact"));
        assertFalse(service.dependencyExists("non.existent", "artifact"));
        
        // Test non-existent profile
        assertThrows(IllegalArgumentException.class, () -> 
            service.dependencyExists("non-existent-profile", "jakarta.platform", "jakarta.jakartaee-api"));
    }

    @Test
    void testFindDependency() throws IOException, XmlPullParserException {
        // Find existing dependency
        Optional<DependencyRecord> found = service.findDependency("jakarta.platform", "jakarta.jakartaee-api");
        assertTrue(found.isPresent());
        assertEquals("jakarta.platform", found.get().groupId());
        assertEquals("jakarta.jakartaee-api", found.get().artifactId());
        assertEquals("provided", found.get().scope());
        
        // Find non-existent dependency
        Optional<DependencyRecord> notFound = service.findDependency("non.existent", "artifact");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testAddDependency() throws IOException, XmlPullParserException {
        // Test adding to main POM with unique name
        String uniqueGroup = "test.unique.group." + System.currentTimeMillis();
        service.addDependency(null, uniqueGroup, "test-artifact", "1.0.0", "jar", "compile");
        assertTrue(service.dependencyExists(null, uniqueGroup, "test-artifact"));
        
        // Test adding to profile with unique name
        String uniqueProfileGroup = "test.profile.group." + System.currentTimeMillis();
        service.addDependency("jakarta-ee", uniqueProfileGroup, "profile-artifact", "2.0.0", "jar", "test");
        assertTrue(service.dependencyExists("jakarta-ee", uniqueProfileGroup, "profile-artifact"));
        
        // Test duplicate prevention
        assertThrows(IllegalArgumentException.class, () -> 
            service.addDependency(null, uniqueGroup, "test-artifact", "1.0.0", "jar", "compile"));
        
        // Test non-existent profile
        assertThrows(IllegalArgumentException.class, () -> 
            service.addDependency("non-existent", "test.group2", "test-artifact2", "1.0.0", "jar", "compile"));
    }

    @Test
    void testUpdateDependencyVersion() throws IOException, XmlPullParserException {
        // Update main POM dependency
        service.updateDependencyVersion(null, "org.junit.jupiter", "junit-jupiter", "5.11.0");
        Optional<DependencyRecord> updated = service.findDependency("org.junit.jupiter", "junit-jupiter");
        assertTrue(updated.isPresent());
        assertEquals("5.11.0", updated.get().version());
        
        // Update profile dependency (use existing test.profile dependency)
        service.updateDependencyVersion("jakarta-ee", "test.profile", "profile-artifact", "3.0.0");
        List<DependencyRecord> allDeps = service.getAllDependencies();
        assertTrue(allDeps.stream().anyMatch(dep -> 
            "jakarta-ee".equals(dep.profile()) && 
            "test.profile".equals(dep.groupId()) && 
            "3.0.0".equals(dep.version())));
        
        // Test non-existent dependency
        assertThrows(IllegalArgumentException.class, () -> 
            service.updateDependencyVersion(null, "non.existent", "artifact", "1.0.0"));
    }

    @Test
    void testRemoveDependency() throws IOException, XmlPullParserException {
        // Remove main POM dependency (use existing jquery dependency)
        assertTrue(service.dependencyExists(null, "org.webjars", "jquery"));
        service.removeDependency(null, "org.webjars", "jquery");
        assertFalse(service.dependencyExists(null, "org.webjars", "jquery"));
        
        // Remove profile dependency (use existing test.profile dependency)
        assertTrue(service.dependencyExists("jakarta-ee", "test.profile", "profile-artifact"));
        service.removeDependency("jakarta-ee", "test.profile", "profile-artifact");
        assertFalse(service.dependencyExists("jakarta-ee", "test.profile", "profile-artifact"));
        
        // Test non-existent dependency
        assertThrows(IllegalArgumentException.class, () -> 
            service.removeDependency(null, "non.existent", "artifact"));
    }

    @Test
    void testAddProperty() throws IOException, XmlPullParserException {
        // Add to main POM
        service.addProperty(null, "test.property", "test.value");
        List<PropertyRecord> properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop -> 
            prop.profile() == null && 
            "test.property".equals(prop.key()) && 
            "test.value".equals(prop.value())));
        
        // Add to profile
        service.addProperty("jakarta-ee", "profile.property", "profile.value");
        properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop -> 
            "jakarta-ee".equals(prop.profile()) && 
            "profile.property".equals(prop.key()) && 
            "profile.value".equals(prop.value())));
        
        // Test duplicate prevention
        assertThrows(IllegalArgumentException.class, () -> 
            service.addProperty(null, "version.java", "18"));
    }

    @Test
    void testUpdatePropertyValue() throws IOException, XmlPullParserException {
        // Update main property
        service.updatePropertyValue(null, "version.java", "21");
        List<PropertyRecord> properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop -> 
            prop.profile() == null && 
            "version.java".equals(prop.key()) && 
            "21".equals(prop.value())));
        
        // Update profile property
        service.updatePropertyValue("jakarta-ee", "version.jakarta.ee", "11.0.0");
        properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop -> 
            "jakarta-ee".equals(prop.profile()) && 
            "version.jakarta.ee".equals(prop.key()) && 
            "11.0.0".equals(prop.value())));
        
        // Test non-existent property
        assertThrows(IllegalArgumentException.class, () -> 
            service.updatePropertyValue(null, "non.existent", "value"));
    }

    @Test
    void testRemoveProperty() throws IOException, XmlPullParserException {
        // Remove main property (use an existing property)
        service.removeProperty(null, "version.mockito");
        List<PropertyRecord> properties = service.getAllProperties();
        assertFalse(properties.stream().anyMatch(prop -> 
            prop.profile() == null && "version.mockito".equals(prop.key())));
        
        // Remove profile property (use an existing property)
        service.removeProperty("jakarta-ee", "profile.property");
        properties = service.getAllProperties();
        assertFalse(properties.stream().anyMatch(prop -> 
            "jakarta-ee".equals(prop.profile()) && "profile.property".equals(prop.key())));
        
        // Test non-existent property
        assertThrows(IllegalArgumentException.class, () -> 
            service.removeProperty(null, "non.existent"));
    }

    @Test
    void testAddDependencyManagementDependency() throws IOException, XmlPullParserException {
        // Add to main dependencyManagement
        service.addDependencyManagementDependency(null, "test.group", "test-bom", "1.0.0", "pom", "import");
        List<DependencyRecord> depMgmt = service.getAllDependencyManagementDependencies();
        assertTrue(depMgmt.stream().anyMatch(dep -> 
            dep.profile() == null && 
            "test.group".equals(dep.groupId()) && 
            "test-bom".equals(dep.artifactId())));
        
        // Add to profile dependencyManagement
        service.addDependencyManagementDependency("jakarta-ee", "profile.group", "profile-bom", "2.0.0", "pom", "import");
        depMgmt = service.getAllDependencyManagementDependencies();
        assertTrue(depMgmt.stream().anyMatch(dep -> 
            "jakarta-ee".equals(dep.profile()) && 
            "profile.group".equals(dep.groupId()) && 
            "profile-bom".equals(dep.artifactId())));
        
        // Test duplicate prevention
        assertThrows(IllegalArgumentException.class, () -> 
            service.addDependencyManagementDependency(null, "org.jboss.arquillian", "arquillian-bom", "1.0.0", "pom", "import"));
    }

    @Test
    void testAddPlugin() throws IOException, XmlPullParserException {
        // Add to main build
        service.addPlugin(null, "org.test", "test-plugin", "1.0.0", true);
        List<PluginRecord> plugins = service.getAllPlugins();
        assertTrue(plugins.stream().anyMatch(plugin -> 
            plugin.profile() == null && 
            "org.test".equals(plugin.groupId()) && 
            "test-plugin".equals(plugin.artifactId())));
        
        // Add to profile build
        service.addPlugin("jacoco", "org.profile", "profile-plugin", "2.0.0", false);
        plugins = service.getAllPlugins();
        assertTrue(plugins.stream().anyMatch(plugin -> 
            "jacoco".equals(plugin.profile()) && 
            "org.profile".equals(plugin.groupId()) && 
            "profile-plugin".equals(plugin.artifactId())));
        
        // Test duplicate prevention
        assertThrows(IllegalArgumentException.class, () -> 
            service.addPlugin(null, null, "maven-compiler-plugin", "3.13.0"));
    }
}