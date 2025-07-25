package org.agoncal.sample.mcp.maven.pomxml;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
        // Test with main POM dependency - org.junit.jupiter:junit-jupiter
        String mainGroupId = "org.junit.jupiter";
        String mainArtifactId = "junit-jupiter";
        String newMainVersion = "5.12.0"; // Updated version
        
        // Get original version
        Optional<DependencyRecord> originalMainDep = service.findDependency(mainGroupId, mainArtifactId);
        assertTrue(originalMainDep.isPresent());
        String originalMainVersion = originalMainDep.get().version();
        
        // Update main POM dependency version
        service.updateDependencyVersion(null, mainGroupId, mainArtifactId, newMainVersion);
        Optional<DependencyRecord> updatedMainDep = service.findDependency(mainGroupId, mainArtifactId);
        assertTrue(updatedMainDep.isPresent());
        assertEquals(newMainVersion, updatedMainDep.get().version());
        
        // Restore original version
        service.updateDependencyVersion(null, mainGroupId, mainArtifactId, originalMainVersion);
        Optional<DependencyRecord> restoredMainDep = service.findDependency(mainGroupId, mainArtifactId);
        assertTrue(restoredMainDep.isPresent());
        assertEquals(originalMainVersion, restoredMainDep.get().version());
        
        // Test with profile dependency - org.apache.derby:derby from jakarta-ee profile
        String profileId = "jakarta-ee";
        String profileGroupId = "org.apache.derby";
        String profileArtifactId = "derby";
        String newProfileVersion = "11.0.0.0"; // Updated version
        
        // Get original profile dependency version
        List<DependencyRecord> allDeps = service.getAllDependencies();
        Optional<DependencyRecord> originalProfileDep = allDeps.stream()
            .filter(dep -> profileId.equals(dep.profile()) && 
                          profileGroupId.equals(dep.groupId()) && 
                          profileArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(originalProfileDep.isPresent());
        String originalProfileVersion = originalProfileDep.get().version();
        
        // Update profile dependency version
        service.updateDependencyVersion(profileId, profileGroupId, profileArtifactId, newProfileVersion);
        allDeps = service.getAllDependencies();
        assertTrue(allDeps.stream().anyMatch(dep -> 
            profileId.equals(dep.profile()) && 
            profileGroupId.equals(dep.groupId()) && 
            profileArtifactId.equals(dep.artifactId()) &&
            newProfileVersion.equals(dep.version())));
        
        // Restore original profile dependency version
        service.updateDependencyVersion(profileId, profileGroupId, profileArtifactId, originalProfileVersion);
        allDeps = service.getAllDependencies();
        assertTrue(allDeps.stream().anyMatch(dep -> 
            profileId.equals(dep.profile()) && 
            profileGroupId.equals(dep.groupId()) && 
            profileArtifactId.equals(dep.artifactId()) &&
            originalProfileVersion.equals(dep.version())));

        // Test non-existent dependency
        assertThrows(IllegalArgumentException.class, () ->
            service.updateDependencyVersion(null, "non.existent", "artifact", "1.0.0"));
    }

    @Test
    void testRemoveAndAddDependency() throws IOException, XmlPullParserException {
        // Test with main POM dependency - bootstrap
        String mainGroupId = "org.webjars";
        String mainArtifactId = "bootstrap";
        String mainVersion = "${version.bootstrap}";
        String mainType = null;
        String mainScope = null; // compile is default

        // Verify dependency exists initially
        assertTrue(service.dependencyExists(null, mainGroupId, mainArtifactId));

        // Get initial dependency details
        Optional<DependencyRecord> originalMainDep = service.findDependency(mainGroupId, mainArtifactId);
        assertTrue(originalMainDep.isPresent());

        // Remove main POM dependency
        service.removeDependency(null, mainGroupId, mainArtifactId);
        assertFalse(service.dependencyExists(null, mainGroupId, mainArtifactId));

        // Add it back with original details
        service.addDependency(null, mainGroupId, mainArtifactId, mainVersion, mainType, mainScope);
        assertTrue(service.dependencyExists(null, mainGroupId, mainArtifactId));

        // Verify it was restored correctly
        Optional<DependencyRecord> restoredMainDep = service.findDependency(mainGroupId, mainArtifactId);
        assertTrue(restoredMainDep.isPresent());
        assertEquals(originalMainDep.get().groupId(), restoredMainDep.get().groupId());
        assertEquals(originalMainDep.get().artifactId(), restoredMainDep.get().artifactId());
        assertEquals(originalMainDep.get().version(), restoredMainDep.get().version());

        // Test with profile dependency - derby from jakarta-ee profile
        String profileId = "jakarta-ee";
        String profileGroupId = "org.apache.derby";
        String profileArtifactId = "derby";
        String profileVersion = "${version.derby}";
        String profileType = null;
        String profileScope = "test";

        // Verify profile dependency exists initially
        assertTrue(service.dependencyExists(profileId, profileGroupId, profileArtifactId));

        // Get all dependencies to find the original profile dependency
        List<DependencyRecord> allDeps = service.getAllDependencies();
        Optional<DependencyRecord> originalProfileDep = allDeps.stream()
            .filter(dep -> profileId.equals(dep.profile()) &&
                          profileGroupId.equals(dep.groupId()) &&
                          profileArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(originalProfileDep.isPresent());

        // Remove profile dependency
        service.removeDependency(profileId, profileGroupId, profileArtifactId);
        assertFalse(service.dependencyExists(profileId, profileGroupId, profileArtifactId));

        // Add it back with original details
        service.addDependency(profileId, profileGroupId, profileArtifactId, profileVersion, profileType, profileScope);
        assertTrue(service.dependencyExists(profileId, profileGroupId, profileArtifactId));

        // Verify it was restored correctly
        allDeps = service.getAllDependencies();
        Optional<DependencyRecord> restoredProfileDep = allDeps.stream()
            .filter(dep -> profileId.equals(dep.profile()) &&
                          profileGroupId.equals(dep.groupId()) &&
                          profileArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(restoredProfileDep.isPresent());
        assertEquals(originalProfileDep.get().groupId(), restoredProfileDep.get().groupId());
        assertEquals(originalProfileDep.get().artifactId(), restoredProfileDep.get().artifactId());
        assertEquals(originalProfileDep.get().version(), restoredProfileDep.get().version());
        assertEquals(originalProfileDep.get().scope(), restoredProfileDep.get().scope());

        // Test duplicate prevention - trying to add the restored dependency again should fail
        assertThrows(IllegalArgumentException.class, () ->
            service.addDependency(null, mainGroupId, mainArtifactId, mainVersion, mainType, mainScope));
        assertThrows(IllegalArgumentException.class, () ->
            service.addDependency(profileId, profileGroupId, profileArtifactId, profileVersion, profileType, profileScope));

        // Test non-existent dependency removal
        assertThrows(IllegalArgumentException.class, () ->
            service.removeDependency(null, "non.existent", "artifact"));
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
    void testRemoveAndAddProperty() throws IOException, XmlPullParserException {
        // Test with main POM property - version.bootstrap
        String mainPropertyKey = "version.bootstrap";
        String mainPropertyValue = "5.3.0";

        // Verify property exists initially
        List<PropertyRecord> properties = service.getAllProperties();
        Optional<PropertyRecord> originalMainProp = properties.stream()
            .filter(prop -> prop.profile() == null && mainPropertyKey.equals(prop.key()))
            .findFirst();
        assertTrue(originalMainProp.isPresent());
        assertEquals(mainPropertyValue, originalMainProp.get().value());

        // Remove main POM property
        service.removeProperty(null, mainPropertyKey);
        properties = service.getAllProperties();
        assertFalse(properties.stream().anyMatch(prop ->
            prop.profile() == null && mainPropertyKey.equals(prop.key())));

        // Add it back with original value
        service.addProperty(null, mainPropertyKey, mainPropertyValue);
        properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop ->
            prop.profile() == null &&
            mainPropertyKey.equals(prop.key()) &&
            mainPropertyValue.equals(prop.value())));

        // Test with profile property - version.arquillian.ee from jakarta-ee profile
        String profileId = "jakarta-ee";
        String profilePropertyKey = "version.arquillian.ee";
        String profilePropertyValue = "10.9.8";

        // Verify profile property exists initially
        properties = service.getAllProperties();
        Optional<PropertyRecord> originalProfileProp = properties.stream()
            .filter(prop -> profileId.equals(prop.profile()) && profilePropertyKey.equals(prop.key()))
            .findFirst();
        assertTrue(originalProfileProp.isPresent());
        assertEquals(profilePropertyValue, originalProfileProp.get().value());

        // Remove profile property
        service.removeProperty(profileId, profilePropertyKey);
        properties = service.getAllProperties();
        assertFalse(properties.stream().anyMatch(prop ->
            profileId.equals(prop.profile()) && profilePropertyKey.equals(prop.key())));

        // Add it back with original value
        service.addProperty(profileId, profilePropertyKey, profilePropertyValue);
        properties = service.getAllProperties();
        assertTrue(properties.stream().anyMatch(prop ->
            profileId.equals(prop.profile()) &&
            profilePropertyKey.equals(prop.key()) &&
            profilePropertyValue.equals(prop.value())));

        // Test duplicate prevention - trying to add the restored property again should fail
        assertThrows(IllegalArgumentException.class, () ->
            service.addProperty(null, mainPropertyKey, mainPropertyValue));
        assertThrows(IllegalArgumentException.class, () ->
            service.addProperty(profileId, profilePropertyKey, profilePropertyValue));

        // Test non-existent property removal
        assertThrows(IllegalArgumentException.class, () ->
            service.removeProperty(null, "non.existent"));
    }

    @Test
    void testRemoveAndAddDependencyInDependencyManagement() throws IOException, XmlPullParserException {
        // Test with main POM dependencyManagement - org.jboss.arquillian:arquillian-bom
        String mainGroupId = "org.jboss.arquillian";
        String mainArtifactId = "arquillian-bom";
        String mainVersion = "${version.arquillian}";
        String mainType = "pom";
        String mainScope = "import";
        
        // Verify dependency exists initially
        List<DependencyRecord> depMgmt = service.getAllDependencyManagementDependencies();
        Optional<DependencyRecord> originalMainDep = depMgmt.stream()
            .filter(dep -> dep.profile() == null && 
                          mainGroupId.equals(dep.groupId()) && 
                          mainArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(originalMainDep.isPresent());
        
        // Remove main POM dependencyManagement dependency
        service.removeDependencyManagementDependency(null, mainGroupId, mainArtifactId);
        depMgmt = service.getAllDependencyManagementDependencies();
        assertFalse(depMgmt.stream().anyMatch(dep -> 
            dep.profile() == null && 
            mainGroupId.equals(dep.groupId()) && 
            mainArtifactId.equals(dep.artifactId())));
        
        // Add it back with original details
        service.addDependencyManagementDependency(null, mainGroupId, mainArtifactId, mainVersion, mainType, mainScope);
        depMgmt = service.getAllDependencyManagementDependencies();
        assertTrue(depMgmt.stream().anyMatch(dep -> 
            dep.profile() == null && 
            mainGroupId.equals(dep.groupId()) && 
            mainArtifactId.equals(dep.artifactId())));
        
        // Verify it was restored correctly
        Optional<DependencyRecord> restoredMainDep = depMgmt.stream()
            .filter(dep -> dep.profile() == null && 
                          mainGroupId.equals(dep.groupId()) && 
                          mainArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(restoredMainDep.isPresent());
        assertEquals(originalMainDep.get().groupId(), restoredMainDep.get().groupId());
        assertEquals(originalMainDep.get().artifactId(), restoredMainDep.get().artifactId());
        assertEquals(originalMainDep.get().version(), restoredMainDep.get().version());
        assertEquals(originalMainDep.get().type(), restoredMainDep.get().type());
        assertEquals(originalMainDep.get().scope(), restoredMainDep.get().scope());
        
        // Test with profile dependencyManagement - org.jboss.arquillian:arquillian-bom from jakarta-ee profile
        String profileId = "jakarta-ee";
        String profileGroupId = "org.jboss.arquillian";
        String profileArtifactId = "arquillian-bom";
        String profileVersion = "${version.arquillian.ee}";
        String profileType = "pom";
        String profileScope = "import";
        
        // Verify profile dependency exists initially
        depMgmt = service.getAllDependencyManagementDependencies();
        Optional<DependencyRecord> originalProfileDep = depMgmt.stream()
            .filter(dep -> profileId.equals(dep.profile()) && 
                          profileGroupId.equals(dep.groupId()) && 
                          profileArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(originalProfileDep.isPresent());
        
        // Remove profile dependencyManagement dependency
        service.removeDependencyManagementDependency(profileId, profileGroupId, profileArtifactId);
        depMgmt = service.getAllDependencyManagementDependencies();
        assertFalse(depMgmt.stream().anyMatch(dep -> 
            profileId.equals(dep.profile()) && 
            profileGroupId.equals(dep.groupId()) && 
            profileArtifactId.equals(dep.artifactId())));
        
        // Add it back with original details
        service.addDependencyManagementDependency(profileId, profileGroupId, profileArtifactId, profileVersion, profileType, profileScope);
        depMgmt = service.getAllDependencyManagementDependencies();
        assertTrue(depMgmt.stream().anyMatch(dep -> 
            profileId.equals(dep.profile()) && 
            profileGroupId.equals(dep.groupId()) && 
            profileArtifactId.equals(dep.artifactId())));
        
        // Verify it was restored correctly
        Optional<DependencyRecord> restoredProfileDep = depMgmt.stream()
            .filter(dep -> profileId.equals(dep.profile()) && 
                          profileGroupId.equals(dep.groupId()) && 
                          profileArtifactId.equals(dep.artifactId()))
            .findFirst();
        assertTrue(restoredProfileDep.isPresent());
        assertEquals(originalProfileDep.get().groupId(), restoredProfileDep.get().groupId());
        assertEquals(originalProfileDep.get().artifactId(), restoredProfileDep.get().artifactId());
        assertEquals(originalProfileDep.get().version(), restoredProfileDep.get().version());
        assertEquals(originalProfileDep.get().type(), restoredProfileDep.get().type());
        assertEquals(originalProfileDep.get().scope(), restoredProfileDep.get().scope());
        
        // Test duplicate prevention - trying to add the restored dependencies again should fail
        assertThrows(IllegalArgumentException.class, () -> 
            service.addDependencyManagementDependency(null, mainGroupId, mainArtifactId, mainVersion, mainType, mainScope));
        assertThrows(IllegalArgumentException.class, () -> 
            service.addDependencyManagementDependency(profileId, profileGroupId, profileArtifactId, profileVersion, profileType, profileScope));
        
        // Test non-existent dependency removal
        assertThrows(IllegalArgumentException.class, () -> 
            service.removeDependencyManagementDependency(null, "non.existent", "non-existent-artifact"));
    }

    @Test
    void testRemoveAndAddPlugin() throws IOException, XmlPullParserException {
        // Test with main POM plugin - maven-surefire-plugin
        String mainGroupId = null; // maven group (defaults to org.apache.maven.plugins)
        String mainArtifactId = "maven-surefire-plugin";
        String mainVersion = "${version.maven.surefire.plugin}";
        Boolean mainInherited = null; // not specified in XML
        
        // Verify plugin exists initially
        List<PluginRecord> plugins = service.getAllPlugins();
        Optional<PluginRecord> originalMainPlugin = plugins.stream()
            .filter(plugin -> plugin.profile() == null && mainArtifactId.equals(plugin.artifactId()))
            .findFirst();
        assertTrue(originalMainPlugin.isPresent());
        
        // Remove main POM plugin
        service.removePlugin(null, mainGroupId, mainArtifactId);
        plugins = service.getAllPlugins();
        assertFalse(plugins.stream().anyMatch(plugin -> 
            plugin.profile() == null && mainArtifactId.equals(plugin.artifactId())));
        
        // Add it back with original details
        service.addPlugin(null, mainGroupId, mainArtifactId, mainVersion, mainInherited);
        plugins = service.getAllPlugins();
        assertTrue(plugins.stream().anyMatch(plugin -> 
            plugin.profile() == null && mainArtifactId.equals(plugin.artifactId())));
        
        // Verify it was restored correctly
        Optional<PluginRecord> restoredMainPlugin = plugins.stream()
            .filter(plugin -> plugin.profile() == null && mainArtifactId.equals(plugin.artifactId()))
            .findFirst();
        assertTrue(restoredMainPlugin.isPresent());
        assertEquals(originalMainPlugin.get().artifactId(), restoredMainPlugin.get().artifactId());
        assertEquals(originalMainPlugin.get().version(), restoredMainPlugin.get().version());
        
        // Test with profile plugin - jacoco-maven-plugin from jacoco profile
        String profileId = "jacoco";
        String profileGroupId = "org.jacoco";
        String profileArtifactId = "jacoco-maven-plugin";
        String profileVersion = "0.8.10";
        Boolean profileInherited = null; // not specified in XML
        
        // Verify profile plugin exists initially
        plugins = service.getAllPlugins();
        Optional<PluginRecord> originalProfilePlugin = plugins.stream()
            .filter(plugin -> profileId.equals(plugin.profile()) && 
                             profileGroupId.equals(plugin.groupId()) &&
                             profileArtifactId.equals(plugin.artifactId()))
            .findFirst();
        assertTrue(originalProfilePlugin.isPresent());
        
        // Remove profile plugin
        service.removePlugin(profileId, profileGroupId, profileArtifactId);
        plugins = service.getAllPlugins();
        assertFalse(plugins.stream().anyMatch(plugin -> 
            profileId.equals(plugin.profile()) && 
            profileGroupId.equals(plugin.groupId()) &&
            profileArtifactId.equals(plugin.artifactId())));
        
        // Add it back with original details
        service.addPlugin(profileId, profileGroupId, profileArtifactId, profileVersion, profileInherited);
        plugins = service.getAllPlugins();
        assertTrue(plugins.stream().anyMatch(plugin -> 
            profileId.equals(plugin.profile()) && 
            profileGroupId.equals(plugin.groupId()) &&
            profileArtifactId.equals(plugin.artifactId())));
        
        // Verify it was restored correctly
        Optional<PluginRecord> restoredProfilePlugin = plugins.stream()
            .filter(plugin -> profileId.equals(plugin.profile()) && 
                             profileGroupId.equals(plugin.groupId()) &&
                             profileArtifactId.equals(plugin.artifactId()))
            .findFirst();
        assertTrue(restoredProfilePlugin.isPresent());
        assertEquals(originalProfilePlugin.get().groupId(), restoredProfilePlugin.get().groupId());
        assertEquals(originalProfilePlugin.get().artifactId(), restoredProfilePlugin.get().artifactId());
        assertEquals(originalProfilePlugin.get().version(), restoredProfilePlugin.get().version());
        
        // Test duplicate prevention - trying to add the restored plugins again should fail
        assertThrows(IllegalArgumentException.class, () -> 
            service.addPlugin(null, mainGroupId, mainArtifactId, mainVersion, mainInherited));
        assertThrows(IllegalArgumentException.class, () -> 
            service.addPlugin(profileId, profileGroupId, profileArtifactId, profileVersion, profileInherited));
        
        // Test non-existent plugin removal
        assertThrows(IllegalArgumentException.class, () -> 
            service.removePlugin(null, "non.existent", "non-existent-plugin"));
    }
}
