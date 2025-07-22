# MCP Maven POM.xml Server

A Model Context Protocol (MCP) server built with Quarkus that provides Maven POM.xml manipulation capabilities. This server exposes MCP tools for reading, adding, updating, and removing Maven POM attributes (properties, dependencies, profiles...).

## Features

- **Property Management**: Add, update, remove, and list Maven POM properties
- **Quarkus-based**: Fast startup and low memory footprint
- **MCP Protocol**: Seamless integration with MCP clients like Claude Desktop
- **Atomic Operations**: Safe file operations with proper error handling
- **Type-safe**: Uses Apache Maven Model API for reliable XML parsing

## Architecture

This MCP server provides four main tools for Maven POM manipulation:

1. **gets_all_the_properties** - Retrieve all existing properties
2. **adds_a_new_property** - Add a new property (prevents duplicates)  
3. **updates_the_value_of_an_existing_property** - Update existing property values
4. **removes_an_existing_property** - Remove existing properties

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

Once configured with an MCP client, you can use the following prompts to interact with the server:

```
Get all the properties of this pom.xml file.

Add a new property "adds_a_new_property" with value: "2.0.0"

Use the version of JUnit to 5.10

Use the property for Arquillian
```

## Example POM Structure

The server works with standard Maven POM files. Here's an example of properties it can manage:

```xml
<properties>
    <version.java>17</version.java>
    <version.jakarta.ee>10.0.0</version.jakarta.ee>
    <version.junit>5.10.0</version.junit>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

## Error Handling

The server provides clear error messages for common scenarios:

- Attempting to add a property that already exists
- Trying to update/remove a non-existent property  
- File I/O errors when reading/writing POM files
- XML parsing errors for malformed POM files

## Dependencies

- **Quarkus**
- **Quarkus MCP Server**: MCP protocol implementation
- **Apache Maven Model**: POM file parsing and manipulation
