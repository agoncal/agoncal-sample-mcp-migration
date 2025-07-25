# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Maven-based Quarkus application that implements a Model Context Protocol (MCP) server for Maven POM.xml manipulation. The server exposes MCP tools for reading, adding, updating, and removing Maven POM attributes including properties, dependencies, plugins, profiles, and dependency management.

## Build and Development Commands

### Building the Application
```bash
mvn clean package
```

### Running Tests
```bash
mvn test
```

### Running Integration Tests
```bash
mvn integration-test
```

### Building Native Image
```bash
mvn clean package -Pnative
```

### Running the Application
```bash
java -jar target/mcp-maven-pomxml-1.0.0-SNAPSHOT-runner.jar
```

### Running in Development Mode
```bash
mvn quarkus:dev
```

## Architecture

### Core Components

**MavenPomXmlMCPServer** (`src/main/java/org/agoncal/sample/mcp/maven/pomxml/MavenPomXmlMCPServer.java:31`) - Main MCP server class that exposes 15 MCP tools for Maven POM manipulation:

- **Property Management**: `gets_all_the_properties`, `adds_a_new_property`, `updates_the_value_of_an_existing_property`, `removes_an_existing_property`
- **Dependency Management**: `gets_all_the_dependencies`, `adds_a_new_dependency`, `updates_the_version_of_an_existing_dependency`, `removes_an_existing_dependency`
- **Dependency Management Section**: `gets_all_the_dependency_management_dependencies`
- **Plugin Management**: `gets_all_the_plugins`, `updates_the_version_of_an_existing_plugin`, `removes_an_existing_plugin`
- **Profile Management**: `gets_all_the_profiles`

### Data Models

The application uses record classes for data transfer:
- **PropertyRecord** - Represents Maven properties (key-value pairs)
- **DependencyRecord** - Represents Maven dependencies with profile context
- **PluginRecord** - Represents Maven plugins with nested dependencies
- **ProfileRecord** - Represents Maven profiles
- **DependencyManagementRecord** - Represents dependency management entries
- **ProjectRecord** - Represents project information

### Key Dependencies

- **Quarkus**: Framework for building cloud-native Java applications
- **Quarkus MCP Server**: MCP protocol implementation for Quarkus
- **Apache Maven Model**: POM file parsing and manipulation API
- **Jackson**: JSON serialization/deserialization

## Configuration

### Environment Variables
- **POM_XML_PATH**: Path to the Maven POM file to manipulate (defaults to test resource file)

### Application Properties
- `quarkus.package.jar.type=uber-jar` - Creates fat JAR for easy deployment
- `quarkus.application.name=MCP Maven POM XML`

## MCP Integration

The server integrates with MCP clients (like Claude Desktop) via configuration in `mcp.json`. The server uses stdio transport and processes requests synchronously.

### Sample MCP Client Configuration
```json
{
  "servers": {
    "maven_pom_xml": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "/path/to/mcp-maven-pomxml-1.0.0-SNAPSHOT-runner.jar"],
      "env": {"POM_XML_PATH": "/path/to/your/pom.xml"}
    }
  }
}
```

## Error Handling

The application provides comprehensive error handling for:
- Duplicate property/dependency/plugin additions
- Non-existent property/dependency/plugin updates/removals
- File I/O errors during POM read/write operations
- XML parsing errors for malformed POM files

## Testing

Test files are located in `src/test/java/org/agoncal/sample/mcp/maven/pomxml/`:
- **PomXmlTest.java** - Manual testing of Maven model operations
- **JsonTest.java** - JSON serialization testing

Sample POM file for testing: `src/test/resources/pomee6.xml` - Contains Jakarta EE 10 Petstore application structure with properties, dependencies, profiles, and plugins.