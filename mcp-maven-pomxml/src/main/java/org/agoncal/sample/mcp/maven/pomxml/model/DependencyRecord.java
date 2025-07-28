package org.agoncal.sample.mcp.maven.pomxml.model;

public record DependencyRecord(String profile, String groupId, String artifactId, String version, String type, String scope) {
}
