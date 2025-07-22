package org.agoncal.sample.mcp.maven.pomxml;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PomXmlTest {

    public static void main(String[] args) throws IOException, XmlPullParserException {

        new MavenPomXmlMCPServer().getAllProperties();

        Path pomPath = Paths.get("mcp-maven-pomxml/src/test/resources/pomee6.xml").toAbsolutePath();

        InputStream inputStream = Files.newInputStream(pomPath);
        MavenXpp3Reader reader = new MavenXpp3Reader();

        if (inputStream == null) {
            throw new IOException("pom.xml not found in classpath");
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            Model model = reader.read(inputStreamReader);

            System.out.println("\n=== READING MODEL");
            System.out.println("=================");
            System.out.println("Group ID: " + model.getGroupId());
            System.out.println("Artifact ID: " + model.getArtifactId());
            System.out.println("Version: " + model.getVersion());
            System.out.println("Packaging: " + model.getPackaging());
            System.out.println("Name: " + model.getName());
            System.out.println("Description: " + model.getDescription());

            model.getContributors().forEach(contributor -> {
                System.out.println("Contributor: " + contributor.getName() + " <" + contributor.getEmail() + ">");
            });
            model.getDependencies().forEach(dependency -> {
                System.out.println("Dependency: " + dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
            });
            model.getDependencyManagement().getDependencies().forEach(dependency -> {
                System.out.println("Dependency Management: " + dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
            });
            model.getProfiles().forEach(profile -> {
                System.out.println("Profile: " + profile.getId());
            });
            model.getBuild().getPlugins().forEach(plugin -> {
                System.out.println("Plugin: " + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
            });
            model.getBuild().getPluginsAsMap().forEach((key, value) -> {
                System.out.println("Plugin Map Entry: " + key + " -> " + value.getGroupId() + ":" + value.getArtifactId() + ":" + value.getVersion());
            });
            model.getProperties().forEach((key, value) -> {
                System.out.println("Property: " + key + " = " + value);
            });


            System.out.println("\n=== UPDATING MODEL");
            System.out.println("==================");
            model.addProperty("new.property", "newValue");
            model.getProperties().forEach((key, value) -> {
                System.out.println("Property: " + key + " = " + value);
            });


            System.out.println("\n=== WRITING MODEL");
            System.out.println("==================");
            MavenXpp3Writer writer = new MavenXpp3Writer();
            try (OutputStream outputStream = Files.newOutputStream(pomPath)) {
                writer.write(outputStream, model);
            }
        }
    }
}
