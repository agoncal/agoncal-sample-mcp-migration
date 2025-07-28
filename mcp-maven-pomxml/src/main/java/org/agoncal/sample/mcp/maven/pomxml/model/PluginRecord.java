package org.agoncal.sample.mcp.maven.pomxml.model;

import java.util.List;

public record PluginRecord(String profile, String groupId, String artifactId, String version, String inherited,
                    List<DependencyRecord> dependencies) {
}
