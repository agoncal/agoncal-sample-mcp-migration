# MCP Maven POM.xml Server

A Model Context Protocol (MCP) server built with Quarkus that provides comprehensive Maven POM.xml manipulation capabilities. This server exposes 16 MCP tools for reading, adding, updating, and removing Maven POM attributes including properties, dependencies, plugins, profiles, parent information, and dependency management.

## Features

- **Comprehensive POM Management**: Complete CRUD operations for all Maven POM elements
- **Profile-Aware Operations**: All mutation operations support profile-specific targeting
- **Property Management**: Add, update, remove, and list Maven POM properties
- **Dependency Management**: Full lifecycle management of dependencies and dependency management sections
- **Plugin Management**: Add, update, remove, and list Maven plugins
- **Parent POM Support**: Read and update parent POM information
- **Profile Support**: List and work with Maven build profiles
- **Quarkus-based**: Fast startup and low memory footprint
- **MCP Protocol**: Seamless integration with MCP clients like Claude Desktop
- **Atomic Operations**: Safe file operations with proper error handling
- **Type-safe**: Uses Apache Maven Model API for reliable XML parsing

## Architecture

### Two-Layer Design Pattern

The application follows a clear separation between the **presentation layer** (MCP server) and the **business logic layer** (service):

1. **MavenDependencyMCPServer** - MCP protocol layer that exposes 16 tools to MCP clients
2. **MavenDependencyService** - Business logic layer that handles all Maven POM manipulation operations

### MCP Tools (16 Total)

This MCP server provides comprehensive Maven POM manipulation through organized tool categories:

**Profile Management**:
- `gets_all_the_profiles` - List all Maven profiles

**Plugin Management**:
- `gets_all_the_plugins` - Retrieve all plugins from main POM and profiles
- `adds_a_new_plugin` - Add new plugins with profile support
- `removes_an_existing_plugin` - Remove plugins from main POM or profiles
- `updates_an_existing_plugin_version` - Update plugin versions

**Dependency Management**:
- `gets_all_the_dependencies` - Retrieve all dependencies
- `adds_a_new_dependency` - Add new dependencies with profile support
- `updates_the_version_of_an_existing_dependency` - Update dependency versions
- `removes_an_existing_dependency` - Remove dependencies

**Dependency Management Section**:
- `gets_all_the_dependency_managements` - Retrieve dependency management entries
- `adds_a_new_dependency_in_dependency_management` - Add dependency management entries
- `removes_an_existing_dependency_management_dependency` - Remove dependency management entries
- `updates_an_existing_dependency_management_dependency_version` - Update dependency management versions

**Property Management**:
- `gets_all_the_properties` - Retrieve all properties
- `adds_a_new_property` - Add new properties (prevents duplicates)
- `updates_the_value_of_an_existing_property` - Update existing property values
- `removes_an_existing_property` - Remove existing properties

**Parent Management**:
- `gets_parent` - Retrieve parent POM information
- `updates_parent_version` - Update parent POM version

### Profile Support

**Critical Feature**: All mutation operations accept a `profileId` parameter:
- `profileId = null` → Operations target the main POM
- `profileId = "profile-name"` → Operations target the specified profile

This enables profile-specific management while maintaining a consistent API.

## Prerequisites

- Java 17 or later
- Maven 3.8.1 or later
- MCP client (such as Claude Desktop)

## Installation

### 1. Clone and Build

```bash
git clone <repository-url>
cd mcp-maven-pomxml
mvn clean package
```

### 2. Configure MCP Client

Add the server to your MCP client configuration (e.g., Claude Desktop):

```json
{
  "mcpServers": {
    "maven_pom_xml": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-maven-pomxml/target/mcp-maven-pomxml-1.0.0-SNAPSHOT-runner.jar"
      ],
      "env": {
        "POM_XML_PATH": "/path/to/your/pom.xml"
      }
    }
  }
}
```

## Configuration

### Environment Variables

- **POM_XML_PATH**: Path to the Maven POM file to manipulate (required)
- **JAVA_HOME**: Java installation directory (optional, if not in PATH)

### Example Configuration

The server includes a sample POM file for testing at `src/test/resources/pomee6.xml` which contains a Jakarta EE 10 Petstore application configuration.

## Usage

Once configured with an MCP client, you can use natural language prompts to interact with the server:

### Property Management
```
Get all the properties of this pom.xml file.
Add a new property "version.junit" with value "5.10.0"
Update the Java version property to "21"
Remove the unused property "old.version"
```

### Dependency Management
```
Show me all dependencies in this POM
Add JUnit Jupiter dependency with version 5.10.0 to test scope
Update Spring Boot version to 3.2.1
Remove the unused Apache Commons dependency
Add a dependency to the "jakarta-ee" profile
```

### Plugin Management
```
List all Maven plugins in this project
Add the Maven Compiler Plugin version 3.11.0
Update the Surefire plugin to version 3.1.2
Remove the outdated plugin from the "production" profile
```

### Profile and Parent Management
```
Show me all build profiles in this POM
Get the parent POM information
Update the parent version to 2.1.0
```

## Example POM Structure

The server works with standard Maven POM files. Here's an example showing the types of elements it can manage:

```xml
<properties>
    <version.java>17</version.java>
    <version.jakarta.ee>10.0.0</version.jakarta.ee>
    <version.junit>5.10.0</version.junit>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${version.junit}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>${version.java}</source>
                <target>${version.java}</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Error Handling

The server provides clear error messages for common scenarios:

- Attempting to add a property that already exists
- Trying to update/remove a non-existent property  
- File I/O errors when reading/writing POM files
- XML parsing errors for malformed POM files

## Key Dependencies

- **Quarkus**: Framework for building cloud-native Java applications
- **Quarkus MCP Server**: MCP protocol implementation for Quarkus (`io.quarkiverse.mcp.server`)
- **Apache Maven Model**: POM file parsing and manipulation API (`org.apache.maven.model`)
- **Jackson**: JSON serialization/deserialization
