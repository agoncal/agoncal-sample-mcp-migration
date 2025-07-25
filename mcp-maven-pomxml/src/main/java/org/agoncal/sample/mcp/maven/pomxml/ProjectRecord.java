package org.agoncal.sample.mcp.maven.pomxml;

import java.util.List;

record ProjectRecord(List<PropertyRecord> properties, DependencyManagementRecord dependencyManagement,
                     List<DependencyRecord> dependencies,
                     List<PluginRecord> plugins, List<ProfileRecord> profiles) {
}
