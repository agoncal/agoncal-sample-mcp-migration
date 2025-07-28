package org.agoncal.sample.mcp.maven.pomxml;

import jakarta.enterprise.context.ApplicationScoped;
import org.agoncal.sample.mcp.maven.pomxml.model.DependencyRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.PluginRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.ProfileRecord;
import org.agoncal.sample.mcp.maven.pomxml.model.PropertyRecord;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class MavenDependencyService {

    private static final Logger log = Logger.getLogger(MavenDependencyService.class);
    private static final String DEFAULT_POM_XML_PATH = "/Users/agoncal/Documents/Code/AGoncal/agoncal-sample-mcp-migration/mcp-maven-pomxml/src/test/resources/pomee6.xml";
    private static final MavenXpp3Reader reader = new MavenXpp3Reader();
    private static final MavenXpp3Writer writer = new MavenXpp3Writer();

    /**
     * Gets the path to the POM file. Can be overridden by subclasses.
     *
     * @return Path to the POM XML file
     */
    protected Path getPomPath() {
        return Paths.get(Optional.ofNullable(System.getenv("POM_XML_PATH")).orElse(DEFAULT_POM_XML_PATH)).toAbsolutePath();
    }

    /**
     * Checks if a profileId represents a null or empty profile.
     * A profile is considered null if it is:
     * - null
     * - equals the string "null"
     * - empty string
     * - contains only whitespace characters
     *
     * @param profileId the profile ID to check
     * @return true if the profile should be treated as null/main POM, false otherwise
     */
    private boolean isProfileNull(String profileId) {
        return profileId == null ||
               "null".equals(profileId.trim()) ||
               profileId.trim().isEmpty();
    }

    /**
     * Retrieves all dependencies from the Maven POM file.
     * This includes dependencies from the main project and all profiles.
     *
     * @return List of DependencyRecord objects representing all dependencies
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    public List<DependencyRecord> getAllDependencies() throws IOException, XmlPullParserException {
        log.info("Getting all dependencies");
        Model model = readModel();

        List<DependencyRecord> dependencies = model.getDependencies().stream()
            .map(dependency -> new DependencyRecord(null,
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getType(),
                dependency.getScope()))
            .collect(Collectors.toList());

        // Add dependencies from profiles
        model.getProfiles().forEach(profile -> {
            profile.getDependencies().stream()
                .map(dependency -> new DependencyRecord(profile.getId(),
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getType(),
                    dependency.getScope()))
                .forEach(dependencies::add);
        });

        return dependencies;
    }

    /**
     * Retrieves all dependencies from the dependencyManagement section.
     * This includes dependencyManagement from the main project and all profiles.
     *
     * @return List of DependencyRecord objects from dependencyManagement
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    public List<DependencyRecord> getAllDependenciesInDependencyManagements() throws IOException, XmlPullParserException {
        log.info("Getting dependency management dependencies");
        Model model = readModel();

        List<DependencyRecord> dependencies = new java.util.ArrayList<>();

        // Add dependencies from main dependencyManagement section
        if (model.getDependencyManagement() != null && !model.getDependencyManagement().getDependencies().isEmpty()) {
            model.getDependencyManagement().getDependencies().stream()
                .map(dependency -> new DependencyRecord(null,
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getType(),
                    dependency.getScope()))
                .forEach(dependencies::add);
        }

        // Add dependencies from dependencyManagement in profiles
        model.getProfiles().forEach(profile -> {
            if (profile.getDependencyManagement() != null && !profile.getDependencyManagement().getDependencies().isEmpty()) {
                profile.getDependencyManagement().getDependencies().stream()
                    .map(dependency -> new DependencyRecord(profile.getId(),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        dependency.getType(),
                        dependency.getScope()))
                    .forEach(dependencies::add);
            }
        });

        return dependencies;
    }

    /**
     * Adds a new dependency to the Maven POM file.
     * Prevents adding duplicate dependencies based on groupId and artifactId.
     *
     * @param profileId  the profile ID to add the dependency to (null for main POM)
     * @param groupId    the group ID of the dependency
     * @param artifactId the artifact ID of the dependency
     * @param version    the version of the dependency
     * @param type       the type of the dependency (optional, defaults to jar)
     * @param scope      the scope of the dependency (optional, defaults to compile)
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the dependency already exists or profile not found
     */
    public void addNewDependency(String profileId, String groupId, String artifactId, String version, String type, String scope)
        throws IOException, XmlPullParserException {
        log.info("Adding dependency: " + groupId + ":" + artifactId + ":" + version +
            (!isProfileNull(profileId) ? " to profile: " + profileId : " to main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Add to main POM dependencies
            boolean exists = model.getDependencies().stream()
                .anyMatch(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId));

            if (exists) {
                throw new IllegalArgumentException("Dependency '" + groupId + ":" + artifactId + "' already exists in main POM");
            }

            Dependency dependency = createDependency(groupId, artifactId, version, type, scope);
            model.addDependency(dependency);
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Check if dependency already exists in the profile
            boolean exists = targetProfile.getDependencies().stream()
                .anyMatch(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId));

            if (exists) {
                throw new IllegalArgumentException("Dependency '" + groupId + ":" + artifactId + "' already exists in profile '" + profileId + "'");
            }

            Dependency dependency = createDependency(groupId, artifactId, version, type, scope);
            targetProfile.addDependency(dependency);
        }

        writeModel(model);
    }

    /**
     * Updates the version of an existing dependency.
     *
     * @param profileId  the profile ID to update the dependency in (null for main POM)
     * @param groupId    the group ID of the dependency to update
     * @param artifactId the artifact ID of the dependency to update
     * @param newVersion the new version to set
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the dependency doesn't exist or profile not found
     */
    public void updateDependencyVersion(String profileId, String groupId, String artifactId, String newVersion)
        throws IOException, XmlPullParserException {
        log.info("Updating dependency version: " + groupId + ":" + artifactId + " to " + newVersion +
            (!isProfileNull(profileId) ? " in profile: " + profileId : " in main POM"));
        Model model = readModel();

        boolean found = false;

        if (isProfileNull(profileId)) {
            // Update in main POM dependencies
            for (Dependency dependency : model.getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                    dependency.setVersion(newVersion);
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Dependency '" + groupId + ":" + artifactId + "' not found in main POM");
            }
        } else {
            // Find the profile
            org.apache.maven.model.Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Update in profile dependencies
            for (Dependency dependency : targetProfile.getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                    dependency.setVersion(newVersion);
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Dependency '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }
        }

        writeModel(model);
    }

    /**
     * Removes an existing dependency from the Maven POM file.
     *
     * @param profileId  the profile ID to remove the dependency from (null for main POM)
     * @param groupId    the group ID of the dependency to remove
     * @param artifactId the artifact ID of the dependency to remove
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the dependency doesn't exist or profile not found
     */
    public void removeExistingDependency(String profileId, String groupId, String artifactId) throws IOException, XmlPullParserException {
        log.info("Removing dependency: " + groupId + ":" + artifactId +
            (!isProfileNull(profileId) ? " from profile: " + profileId : " from main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Remove from main POM dependencies
            Dependency toRemove = null;
            for (Dependency dependency : model.getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                    toRemove = dependency;
                    break;
                }
            }

            if (toRemove == null) {
                throw new IllegalArgumentException("Dependency '" + groupId + ":" + artifactId + "' not found in main POM");
            }

            model.removeDependency(toRemove);
        } else {
            // Find the profile
            org.apache.maven.model.Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Remove from profile dependencies
            Dependency toRemove = null;
            for (Dependency dependency : targetProfile.getDependencies()) {
                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                    toRemove = dependency;
                    break;
                }
            }

            if (toRemove == null) {
                throw new IllegalArgumentException("Dependency '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }

            targetProfile.removeDependency(toRemove);
        }

        writeModel(model);
    }

    /**
     * Checks if a dependency exists in the Maven POM file.
     *
     * @param profileId  the profile ID to check in (null for main POM)
     * @param groupId    the group ID to check for
     * @param artifactId the artifact ID to check for
     * @return true if the dependency exists, false otherwise
     * @throws IOException              if there's an error reading the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the profile doesn't exist
     */
    boolean dependencyExists(String profileId, String groupId, String artifactId) throws IOException, XmlPullParserException {
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Check in main POM dependencies
            return model.getDependencies().stream()
                .anyMatch(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId));
        } else {
            // Find the profile
            org.apache.maven.model.Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Check in profile dependencies
            return targetProfile.getDependencies().stream()
                .anyMatch(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId));
        }
    }

    /**
     * Checks if a dependency exists in the main Maven POM file.
     * Convenience method that checks dependency in the main POM (not in any profile).
     *
     * @param groupId    the group ID to check for
     * @param artifactId the artifact ID to check for
     * @return true if the dependency exists, false otherwise
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    boolean dependencyExists(String groupId, String artifactId) throws IOException, XmlPullParserException {
        return dependencyExists(null, groupId, artifactId);
    }

    /**
     * Finds a specific dependency by groupId and artifactId.
     *
     * @param groupId    the group ID to search for
     * @param artifactId the artifact ID to search for
     * @return Optional containing the DependencyRecord if found, empty otherwise
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    Optional<DependencyRecord> getDependency(String groupId, String artifactId)
        throws IOException, XmlPullParserException {
        Model model = readModel();

        return model.getDependencies().stream()
            .filter(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId))
            .map(dependency -> new DependencyRecord(null,
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getType(),
                dependency.getScope()))
            .findFirst();
    }

    /**
     * Gets dependencies filtered by scope from the main POM and all profiles.
     *
     * @param scope the scope to filter by (e.g., "test", "provided", "compile")
     * @return List of dependencies with the specified scope from main POM and all profiles
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    List<DependencyRecord> getAllDependenciesByScope(String scope) throws IOException, XmlPullParserException {
        log.info("Getting all dependencies with scope: " + scope);
        Model model = readModel();

        List<DependencyRecord> dependencies = new java.util.ArrayList<>();

        // Add dependencies from main POM with matching scope
        model.getDependencies().stream()
            .filter(dep -> {
                String depScope = dep.getScope();
                if (depScope == null) depScope = "compile"; // Maven default
                return depScope.equals(scope);
            })
            .map(dependency -> new DependencyRecord(null,
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getType(),
                dependency.getScope()))
            .forEach(dependencies::add);

        // Add dependencies from profiles with matching scope
        model.getProfiles().forEach(profile -> {
            profile.getDependencies().stream()
                .filter(dep -> {
                    String depScope = dep.getScope();
                    if (depScope == null) depScope = "compile"; // Maven default
                    return depScope.equals(scope);
                })
                .map(dependency -> new DependencyRecord(profile.getId(),
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getType(),
                    dependency.getScope()))
                .forEach(dependencies::add);
        });

        return dependencies;
    }

    /**
     * Retrieves all properties from the Maven POM file.
     * This includes properties from the main project and all profiles.
     *
     * @return List of PropertyRecord objects representing all properties
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    public List<PropertyRecord> getAllProperties() throws IOException, XmlPullParserException {
        log.info("Getting all properties");
        Model model = readModel();

        List<PropertyRecord> properties = new java.util.ArrayList<>();

        // Add properties from main POM
        model.getProperties().entrySet().stream()
            .map(entry -> new PropertyRecord(null, (String) entry.getKey(), (String) entry.getValue()))
            .forEach(properties::add);

        // Add properties from profiles
        model.getProfiles().forEach(profile -> {
            if (profile.getProperties() != null && !profile.getProperties().isEmpty()) {
                profile.getProperties().entrySet().stream()
                    .map(entry -> new PropertyRecord(profile.getId(), (String) entry.getKey(), (String) entry.getValue()))
                    .forEach(properties::add);
            }
        });

        return properties;
    }

    /**
     * Retrieves all dependencyManagement dependencies from the Maven POM file.
     * This includes dependencyManagement from the main project and all profiles.
     *
     * @return List of DependencyRecord objects from all dependencyManagement sections
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    public List<DependencyRecord> getAllDependencyManagements() throws IOException, XmlPullParserException {
        log.info("Getting all dependency management dependencies");
        Model model = readModel();

        List<DependencyRecord> dependencies = new java.util.ArrayList<>();

        // Add dependencies from main dependencyManagement section
        if (model.getDependencyManagement() != null && !model.getDependencyManagement().getDependencies().isEmpty()) {
            model.getDependencyManagement().getDependencies().stream()
                .map(dependency -> new DependencyRecord(null,
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getType(),
                    dependency.getScope()))
                .forEach(dependencies::add);
        }

        // Add dependencies from dependencyManagement in profiles
        model.getProfiles().forEach(profile -> {
            if (profile.getDependencyManagement() != null && !profile.getDependencyManagement().getDependencies().isEmpty()) {
                profile.getDependencyManagement().getDependencies().stream()
                    .map(dependency -> new DependencyRecord(profile.getId(),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        dependency.getType(),
                        dependency.getScope()))
                    .forEach(dependencies::add);
            }
        });

        return dependencies;
    }

    /**
     * Retrieves all profiles from the Maven POM file.
     *
     * @return List of ProfileRecord objects representing all profiles
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    public List<ProfileRecord> getAllProfiles() throws IOException, XmlPullParserException {
        log.info("Getting all profiles");
        Model model = readModel();

        return model.getProfiles().stream()
            .map(profile -> new ProfileRecord(profile.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all plugins from the Maven POM file.
     * This includes plugins from the main project build section and all profiles.
     *
     * @return List of PluginRecord objects representing all plugins
     * @throws IOException            if there's an error reading the POM file
     * @throws XmlPullParserException if there's an error parsing the XML
     */
    public List<PluginRecord> getAllPlugins() throws IOException, XmlPullParserException {
        log.info("Getting all plugins");
        Model model = readModel();

        List<PluginRecord> plugins = new java.util.ArrayList<>();

        // Add plugins from main POM build section
        if (model.getBuild() != null && !model.getBuild().getPlugins().isEmpty()) {
            model.getBuild().getPlugins().stream()
                .map(plugin -> {
                    List<DependencyRecord> dependencies = plugin.getDependencies() != null ?
                        plugin.getDependencies().stream()
                            .map(dependency -> new DependencyRecord(null,
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion(),
                                dependency.getType(),
                                dependency.getScope()))
                            .collect(Collectors.toList()) : List.of();
                    return new PluginRecord(null,
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        plugin.getVersion(),
                        String.valueOf(plugin.isInherited()),
                        dependencies);
                })
                .forEach(plugins::add);
        }

        // Add plugins from profiles
        model.getProfiles().forEach(profile -> {
            if (profile.getBuild() != null && !profile.getBuild().getPlugins().isEmpty()) {
                profile.getBuild().getPlugins().stream()
                    .map(plugin -> {
                        List<DependencyRecord> dependencies = plugin.getDependencies() != null ?
                            plugin.getDependencies().stream()
                                .map(dependency -> new DependencyRecord(profile.getId(),
                                    dependency.getGroupId(),
                                    dependency.getArtifactId(),
                                    dependency.getVersion(),
                                    dependency.getType(),
                                    dependency.getScope()))
                                .collect(Collectors.toList()) : List.of();
                        return new PluginRecord(profile.getId(),
                            plugin.getGroupId(),
                            plugin.getArtifactId(),
                            plugin.getVersion(),
                            String.valueOf(plugin.isInherited()),
                            dependencies);
                    })
                    .forEach(plugins::add);
            }
        });

        return plugins;
    }

    /**
     * Removes an existing property from the Maven POM file.
     *
     * @param profileId the profile ID to remove the property from (null for main POM)
     * @param key       the property key to remove
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the property doesn't exist or profile not found
     */
    public void removeExistingProperty(String profileId, String key) throws IOException, XmlPullParserException {
        log.info("Removing property: " + key +
            (!isProfileNull(profileId) ? " from profile: " + profileId : " from main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Remove from main POM properties
            if (!model.getProperties().containsKey(key)) {
                throw new IllegalArgumentException("Property '" + key + "' not found in main POM");
            }
            model.getProperties().remove(key);
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Remove from profile properties
            if (targetProfile.getProperties() == null || !targetProfile.getProperties().containsKey(key)) {
                throw new IllegalArgumentException("Property '" + key + "' not found in profile '" + profileId + "'");
            }
            targetProfile.getProperties().remove(key);
        }

        writeModel(model);
    }

    /**
     * Updates the value of an existing property in the Maven POM file.
     *
     * @param profileId the profile ID to update the property in (null for main POM)
     * @param key       the property key to update
     * @param value     the new property value
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the property doesn't exist or profile not found
     */
    public void updatePropertyValue(String profileId, String key, String value) throws IOException, XmlPullParserException {
        log.info("Updating property: " + key + " to " + value +
            (!isProfileNull(profileId) ? " in profile: " + profileId : " in main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Update in main POM properties
            if (!model.getProperties().containsKey(key)) {
                throw new IllegalArgumentException("Property '" + key + "' not found in main POM");
            }
            model.getProperties().put(key, value);
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Update in profile properties
            if (targetProfile.getProperties() == null || !targetProfile.getProperties().containsKey(key)) {
                throw new IllegalArgumentException("Property '" + key + "' not found in profile '" + profileId + "'");
            }
            targetProfile.getProperties().put(key, value);
        }

        writeModel(model);
    }

    /**
     * Adds a new property to the Maven POM file.
     * Prevents adding duplicate properties based on the property key.
     *
     * @param profileId the profile ID to add the property to (null for main POM)
     * @param key       the property key to add
     * @param value     the property value to add
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the property already exists or profile not found
     */
    public void addNewProperty(String profileId, String key, String value) throws IOException, XmlPullParserException {
        log.info("Adding property: " + key + " = " + value +
            (!isProfileNull(profileId) ? " to profile: " + profileId : " to main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Add to main POM properties
            if (model.getProperties().containsKey(key)) {
                throw new IllegalArgumentException("Property '" + key + "' already exists in main POM");
            }
            model.addProperty(key, value);
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Initialize properties if null
            if (targetProfile.getProperties() == null) {
                targetProfile.setProperties(new java.util.Properties());
            }

            // Check if property already exists in the profile
            if (targetProfile.getProperties().containsKey(key)) {
                throw new IllegalArgumentException("Property '" + key + "' already exists in profile '" + profileId + "'");
            }

            targetProfile.addProperty(key, value);
        }

        writeModel(model);
    }

    /**
     * Adds a new dependency to the dependencyManagement section of the Maven POM file.
     * Prevents adding duplicate dependencies based on groupId and artifactId.
     *
     * @param profileId  the profile ID to add the dependency to (null for main POM)
     * @param groupId    the group ID of the dependency
     * @param artifactId the artifact ID of the dependency
     * @param version    the version of the dependency
     * @param type       the type of the dependency (optional, defaults to jar)
     * @param scope      the scope of the dependency (optional, defaults to compile)
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the dependency already exists or profile not found
     */
    public void addNewDependencyInDependencyManagement(String profileId, String groupId, String artifactId, String version, String type, String scope)
        throws IOException, XmlPullParserException {
        log.info("Adding dependencyManagement dependency: " + groupId + ":" + artifactId + ":" + version +
            (!isProfileNull(profileId) ? " to profile: " + profileId : " to main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Add to main POM dependencyManagement
            if (model.getDependencyManagement() == null) {
                model.setDependencyManagement(new org.apache.maven.model.DependencyManagement());
            }

            // Check if dependency already exists in main dependencyManagement
            boolean exists = model.getDependencyManagement().getDependencies().stream()
                .anyMatch(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId));

            if (exists) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' already exists in main POM");
            }

            Dependency dependency = createDependency(groupId, artifactId, version, type, scope);
            model.getDependencyManagement().addDependency(dependency);
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Initialize dependencyManagement if null
            if (targetProfile.getDependencyManagement() == null) {
                targetProfile.setDependencyManagement(new org.apache.maven.model.DependencyManagement());
            }

            // Check if dependency already exists in the profile's dependencyManagement
            boolean exists = targetProfile.getDependencyManagement().getDependencies().stream()
                .anyMatch(dep -> dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId));

            if (exists) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' already exists in profile '" + profileId + "'");
            }

            Dependency dependency = createDependency(groupId, artifactId, version, type, scope);
            targetProfile.getDependencyManagement().addDependency(dependency);
        }

        writeModel(model);
    }

    /**
     * Removes an existing dependency from the dependencyManagement section of the Maven POM file.
     *
     * @param profileId  the profile ID to remove the dependency from (null for main POM)
     * @param groupId    the group ID of the dependency
     * @param artifactId the artifact ID of the dependency
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the dependency doesn't exist or profile not found
     */
    public void removeExistingDependencyInDependencyManagement(String profileId, String groupId, String artifactId)
        throws IOException, XmlPullParserException {
        log.info("Removing dependencyManagement dependency: " + groupId + ":" + artifactId +
            (!isProfileNull(profileId) ? " from profile: " + profileId : " from main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Remove from main POM dependencyManagement
            if (model.getDependencyManagement() == null || model.getDependencyManagement().getDependencies().isEmpty()) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' not found in main POM");
            }

            // Find and remove the dependency from main dependencyManagement
            boolean removed = model.getDependencyManagement().getDependencies().removeIf(dep ->
                groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId()));

            if (!removed) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' not found in main POM");
            }
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Check if profile has dependencyManagement section and dependencies
            if (targetProfile.getDependencyManagement() == null || targetProfile.getDependencyManagement().getDependencies().isEmpty()) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }

            // Find and remove the dependency from profile dependencyManagement
            boolean removed = targetProfile.getDependencyManagement().getDependencies().removeIf(dep ->
                groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId()));

            if (!removed) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }
        }

        writeModel(model);
    }

    /**
     * Updates the version of an existing dependency in the dependencyManagement section of the Maven POM file.
     *
     * @param profileId  the profile ID to update the dependency in (null for main POM)
     * @param groupId    the group ID of the dependency to update
     * @param artifactId the artifact ID of the dependency to update
     * @param newVersion the new version to set
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the dependency doesn't exist or profile not found
     */
    public void updateDependencyManagementDependencyVersion(String profileId, String groupId, String artifactId, String newVersion)
        throws IOException, XmlPullParserException {
        log.info("Updating dependencyManagement dependency version: " + groupId + ":" + artifactId + " to " + newVersion +
            (!isProfileNull(profileId) ? " in profile: " + profileId : " in main POM"));
        Model model = readModel();

        boolean found = false;

        if (isProfileNull(profileId)) {
            // Update in main POM dependencyManagement
            if (model.getDependencyManagement() != null && !model.getDependencyManagement().getDependencies().isEmpty()) {
                for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
                    if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                        dependency.setVersion(newVersion);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' not found in main POM");
            }
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Update in profile dependencyManagement
            if (targetProfile.getDependencyManagement() != null && !targetProfile.getDependencyManagement().getDependencies().isEmpty()) {
                for (Dependency dependency : targetProfile.getDependencyManagement().getDependencies()) {
                    if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                        dependency.setVersion(newVersion);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                throw new IllegalArgumentException("DependencyManagement dependency '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }
        }

        writeModel(model);
    }

    /**
     * Adds a new plugin to the build section of the Maven POM file.
     * Prevents adding duplicate plugins based on groupId and artifactId.
     *
     * @param profileId  the profile ID to add the plugin to (null for main POM)
     * @param groupId    the group ID of the plugin
     * @param artifactId the artifact ID of the plugin
     * @param version    the version of the plugin
     * @param inherited  whether the plugin is inherited (optional, defaults to true)
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the plugin already exists or profile not found
     */
    public void addNewPlugin(String profileId, String groupId, String artifactId, String version, Boolean inherited)
        throws IOException, XmlPullParserException {
        log.info("Adding plugin: " + groupId + ":" + artifactId + ":" + version +
            (!isProfileNull(profileId) ? " to profile: " + profileId : " to main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Add to main POM build section
            if (model.getBuild() == null) {
                model.setBuild(new org.apache.maven.model.Build());
            }

            // Check if plugin already exists in main build
            boolean exists = model.getBuild().getPlugins().stream()
                .anyMatch(plugin -> {
                    // Handle null groupId case - Maven plugins default to org.apache.maven.plugins
                    String pluginGroupId = plugin.getGroupId();
                    if (pluginGroupId == null) {
                        pluginGroupId = "org.apache.maven.plugins";
                    }
                    String targetGroupId = groupId;
                    if (targetGroupId == null) {
                        targetGroupId = "org.apache.maven.plugins";
                    }
                    return targetGroupId.equals(pluginGroupId) && plugin.getArtifactId().equals(artifactId);
                });

            if (exists) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' already exists in main POM");
            }

            Plugin plugin = createPlugin(groupId, artifactId, version, inherited);
            model.getBuild().addPlugin(plugin);
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Initialize build if null
            if (targetProfile.getBuild() == null) {
                targetProfile.setBuild(new org.apache.maven.model.Build());
            }

            // Check if plugin already exists in the profile's build
            boolean exists = targetProfile.getBuild().getPlugins().stream()
                .anyMatch(plugin -> {
                    // Handle null groupId case - Maven plugins default to org.apache.maven.plugins
                    String pluginGroupId = plugin.getGroupId();
                    if (pluginGroupId == null) {
                        pluginGroupId = "org.apache.maven.plugins";
                    }
                    String targetGroupId = groupId;
                    if (targetGroupId == null) {
                        targetGroupId = "org.apache.maven.plugins";
                    }
                    return targetGroupId.equals(pluginGroupId) && plugin.getArtifactId().equals(artifactId);
                });

            if (exists) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' already exists in profile '" + profileId + "'");
            }

            Plugin plugin = createPlugin(groupId, artifactId, version, inherited);
            targetProfile.getBuild().addPlugin(plugin);
        }

        writeModel(model);
    }

    /**
     * Removes an existing plugin from the Maven POM file.
     *
     * @param profileId  the profile ID to remove the plugin from (null for main POM)
     * @param groupId    the group ID of the plugin
     * @param artifactId the artifact ID of the plugin
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the plugin doesn't exist or profile not found
     */
    public void removeExistingPlugin(String profileId, String groupId, String artifactId) throws IOException, XmlPullParserException {
        log.info("Removing plugin: " + groupId + ":" + artifactId +
            (!isProfileNull(profileId) ? " from profile: " + profileId : " from main POM"));
        Model model = readModel();

        if (isProfileNull(profileId)) {
            // Remove from main POM build section
            if (model.getBuild() == null || model.getBuild().getPlugins().isEmpty()) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' not found in main POM");
            }

            // Find and remove the plugin from main build
            boolean removed = model.getBuild().getPlugins().removeIf(plugin -> {
                // Handle null groupId case - Maven plugins default to org.apache.maven.plugins
                String pluginGroupId = plugin.getGroupId();
                if (pluginGroupId == null) {
                    pluginGroupId = "org.apache.maven.plugins";
                }
                String targetGroupId = groupId;
                if (targetGroupId == null) {
                    targetGroupId = "org.apache.maven.plugins";
                }
                return targetGroupId.equals(pluginGroupId) && artifactId.equals(plugin.getArtifactId());
            });

            if (!removed) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' not found in main POM");
            }
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Check if profile has build section and plugins
            if (targetProfile.getBuild() == null || targetProfile.getBuild().getPlugins().isEmpty()) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }

            // Find and remove the plugin from profile build
            boolean removed = targetProfile.getBuild().getPlugins().removeIf(plugin -> {
                // Handle null groupId case - Maven plugins default to org.apache.maven.plugins
                String pluginGroupId = plugin.getGroupId();
                if (pluginGroupId == null) {
                    pluginGroupId = "org.apache.maven.plugins";
                }
                String targetGroupId = groupId;
                if (targetGroupId == null) {
                    targetGroupId = "org.apache.maven.plugins";
                }
                return targetGroupId.equals(pluginGroupId) && artifactId.equals(plugin.getArtifactId());
            });

            if (!removed) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }
        }

        writeModel(model);
    }

    /**
     * Updates the version of an existing plugin in the Maven POM file.
     *
     * @param profileId  the profile ID to update the plugin in (null for main POM)
     * @param groupId    the group ID of the plugin to update
     * @param artifactId the artifact ID of the plugin to update
     * @param newVersion the new version to set
     * @throws IOException              if there's an error reading/writing the POM file
     * @throws XmlPullParserException   if there's an error parsing the XML
     * @throws IllegalArgumentException if the plugin doesn't exist or profile not found
     */
    public void updatePluginVersion(String profileId, String groupId, String artifactId, String newVersion)
        throws IOException, XmlPullParserException {
        log.info("Updating plugin version: " + groupId + ":" + artifactId + " to " + newVersion +
            (!isProfileNull(profileId) ? " in profile: " + profileId : " in main POM"));
        Model model = readModel();

        boolean found = false;

        if (isProfileNull(profileId)) {
            // Update in main POM build section
            if (model.getBuild() != null && !model.getBuild().getPlugins().isEmpty()) {
                for (Plugin plugin : model.getBuild().getPlugins()) {
                    // Handle null groupId case - Maven plugins default to org.apache.maven.plugins
                    String pluginGroupId = plugin.getGroupId();
                    if (pluginGroupId == null) {
                        pluginGroupId = "org.apache.maven.plugins";
                    }
                    String targetGroupId = groupId;
                    if (targetGroupId == null) {
                        targetGroupId = "org.apache.maven.plugins";
                    }

                    if (targetGroupId.equals(pluginGroupId) && plugin.getArtifactId().equals(artifactId)) {
                        plugin.setVersion(newVersion);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' not found in main POM");
            }
        } else {
            // Find the profile
            Profile targetProfile = model.getProfiles().stream()
                .filter(profile -> profile.getId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile '" + profileId + "' not found"));

            // Update in profile build section
            if (targetProfile.getBuild() != null && !targetProfile.getBuild().getPlugins().isEmpty()) {
                for (Plugin plugin : targetProfile.getBuild().getPlugins()) {
                    // Handle null groupId case - Maven plugins default to org.apache.maven.plugins
                    String pluginGroupId = plugin.getGroupId();
                    if (pluginGroupId == null) {
                        pluginGroupId = "org.apache.maven.plugins";
                    }
                    String targetGroupId = groupId;
                    if (targetGroupId == null) {
                        targetGroupId = "org.apache.maven.plugins";
                    }

                    if (targetGroupId.equals(pluginGroupId) && plugin.getArtifactId().equals(artifactId)) {
                        plugin.setVersion(newVersion);
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Plugin '" + groupId + ":" + artifactId + "' not found in profile '" + profileId + "'");
            }
        }

        writeModel(model);
    }

    /**
     * Helper method to create a Plugin object with the specified parameters.
     */
    private Plugin createPlugin(String groupId, String artifactId, String version, Boolean inherited) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        if (inherited != null) {
            plugin.setInherited(inherited);
        }
        return plugin;
    }

    /**
     * Helper method to create a Dependency object with the specified parameters.
     */
    private Dependency createDependency(String groupId, String artifactId, String version, String type, String scope) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        if (type != null && !type.isEmpty() && !"jar".equals(type)) {
            dependency.setType(type);
        }
        if (scope != null && !scope.isEmpty() && !"compile".equals(scope)) {
            dependency.setScope(scope);
        }
        return dependency;
    }

    private Model readModel() throws IOException, XmlPullParserException {
        Path pomXmlPath = getPomPath();
        try (InputStreamReader inputStreamReader = new InputStreamReader(Files.newInputStream(pomXmlPath))) {
            return reader.read(inputStreamReader);
        }
    }

    private void writeModel(Model model) throws IOException {
        Path pomXmlPath = getPomPath();
        try (OutputStream outputStream = Files.newOutputStream(pomXmlPath)) {
            writer.write(outputStream, model);
        }
    }
}
