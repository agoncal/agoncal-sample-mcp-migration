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

### Running a Single Test Class
```bash
mvn test -Dtest=MavenDependencyServiceTest
```

### Running a Single Test Method
```bash
mvn test -Dtest=MavenDependencyServiceTest#testAddProperty
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

### Compilation Only
```bash
mvn clean compile
```

## Architecture

### Two-Layer Design Pattern

The application follows a clear separation between the **presentation layer** (MCP server) and the **business logic layer** (service):

1. **MavenDependencyMCPServer** - MCP protocol layer that exposes 12 tools to MCP clients
2. **MavenDependencyService** - Business logic layer that handles all Maven POM manipulation operations

### Core Components

**MavenDependencyMCPServer** (`src/main/java/org/agoncal/sample/mcp/maven/pomxml/MavenDependencyMCPServer.java`) - Main MCP server class that exposes 12 MCP tools for Maven POM manipulation:

- **Profile Management**: `gets_all_the_profiles`
- **Plugin Management**: `gets_all_the_plugins`, `removes_an_existing_plugin`
- **Dependency Management**: `gets_all_the_dependencies`, `adds_a_new_dependency`, `updates_the_version_of_an_existing_dependency`, `removes_an_existing_dependency`
- **Dependency Management Section**: `gets_all_the_dependency_management_dependencies`
- **Property Management**: `gets_all_the_properties`, `adds_a_new_property`, `updates_the_value_of_an_existing_property`, `removes_an_existing_property`

**MavenDependencyService** (`src/main/java/org/agoncal/sample/mcp/maven/pomxml/MavenDependencyService.java`) - Core business logic service that handles:
- Maven POM file reading/writing using Apache Maven Model API
- Profile-aware operations (all mutation operations support profileId parameter)
- Comprehensive CRUD operations for dependencies, properties, plugins, and dependency management
- Atomic file operations with proper error handling

### Profile Support Architecture

**Critical Design Pattern**: All mutation operations (add, update, remove) in both the MCP server and service layers accept a `profileId` parameter:
- `profileId = null` → Operations target the main POM
- `profileId = "profile-name"` → Operations target the specified profile

This enables profile-specific management of properties, dependencies, and plugins while maintaining a consistent API across all operations.

### Data Models

The application uses record classes for data transfer:
- **PropertyRecord** - Represents Maven properties with profile context (key, value, profile)
- **DependencyRecord** - Represents Maven dependencies with profile context (groupId, artifactId, version, scope, type, profile)
- **PluginRecord** - Represents Maven plugins with profile context (groupId, artifactId, version, inherited, profile)
- **ProfileRecord** - Represents Maven profiles (id)
- **DependencyManagementRecord** - Represents dependency management entries
- **ProjectRecord** - Represents project information

### Key Dependencies

- **Quarkus**: Framework for building cloud-native Java applications
- **Quarkus MCP Server**: MCP protocol implementation for Quarkus (`io.quarkiverse.mcp.server`)
- **Apache Maven Model**: POM file parsing and manipulation API (`org.apache.maven.model`)
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

### Test Structure
Test files are located in `src/test/java/org/agoncal/sample/mcp/maven/pomxml/`:
- **MavenDependencyServiceTest.java** - Comprehensive unit tests for the service layer (14 test methods)
- **MavenEmptyPomTest.java** - Tests for handling empty POM files (4 test methods)  
- **PomXmlTest.java** - Manual testing of Maven model operations
- **JsonTest.java** - JSON serialization testing

### Test Resources
- **pomee6.xml** - Sample Jakarta EE 10 Petstore application POM with properties, dependencies, profiles, and plugins for testing

### Running Specific Tests
The comprehensive test suite validates all CRUD operations across profiles and includes edge case handling for non-existent elements.