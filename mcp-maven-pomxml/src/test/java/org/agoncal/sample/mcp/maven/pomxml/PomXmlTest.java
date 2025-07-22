package org.agoncal.sample.mcp.maven.pomxml;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PomXmlTest {

    public static void main(String[] args) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        InputStream inputStream = PomXmlTest.class.getClassLoader().getResourceAsStream("pom.xml");
        
        if (inputStream == null) {
            throw new IOException("pom.xml not found in classpath");
        }
        
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            Model model = reader.read(inputStreamReader);
            
            System.out.println("Group ID: " + model.getGroupId());
            System.out.println("Artifact ID: " + model.getArtifactId());
            System.out.println("Version: " + model.getVersion());
            System.out.println("Packaging: " + model.getPackaging());
            System.out.println("Name: " + model.getName());
            System.out.println("Description: " + model.getDescription());
        }
    }
}
