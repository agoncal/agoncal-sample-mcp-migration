package org.agoncal.sample.mcp.migration.openrewrite;

import java.util.List;

record RecipeJson(String migration, String fqn, String name, String displayName, String description,
                  List<OptionJson> options) {
}
