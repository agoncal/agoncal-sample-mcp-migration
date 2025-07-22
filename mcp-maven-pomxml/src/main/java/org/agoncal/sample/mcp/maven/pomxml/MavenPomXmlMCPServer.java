package org.agoncal.sample.mcp.maven.pomxml;

import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenPomXmlMCPServer {

    private static final Logger log = Logger.getLogger(MavenPomXmlMCPServer.class);
    private static final String ROOT_APP_TO_MIGRATE = System.getenv("ROOT_APP_TO_MIGRATE");
    private static final Path ROOT_PATH = Paths.get(ROOT_APP_TO_MIGRATE);
    private static final File ROOT_DIRECTORY = Paths.get(ROOT_APP_TO_MIGRATE).toFile();

}

