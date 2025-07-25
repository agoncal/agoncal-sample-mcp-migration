package org.agoncal.sample.mcp.maven.pomxml;

import java.util.List;

record PluginRecord(String profile, String groupId, String artifactId, String version, String inherited,
                    List<DependencyRecord> dependencies) {
}
