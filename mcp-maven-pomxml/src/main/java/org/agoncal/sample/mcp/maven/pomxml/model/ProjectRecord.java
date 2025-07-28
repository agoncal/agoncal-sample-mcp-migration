package org.agoncal.sample.mcp.maven.pomxml.model;

import java.util.List;

public record ProjectRecord(List<PropertyRecord> properties, DependencyManagementRecord dependencyManagement,
                     List<DependencyRecord> dependencies,
                     List<PluginRecord> plugins, List<ProfileRecord> profiles) {
}
