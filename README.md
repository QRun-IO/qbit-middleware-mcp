# QBit Middleware MCP

A [QQQ Framework](https://github.com/Kingsrook/qqq) qBit that exposes QQQ-backed applications as
[Model Context Protocol (MCP)](https://modelcontextprotocol.io) servers, enabling AI assistants like
Claude Desktop to discover and interact with your application's data and processes.

## Features

- **Tools** — query, count, get, insert, update, and delete records; execute QQQ processes
- **Resources** — browse table lists and schemas via `qqq://` URIs
- **Prompts** — guided exploration and summarization of table data
- **Security** — read-only by default; write operations are opt-in; pluggable session providers
- **Transport** — STDIO (for Claude Desktop) or Streamable HTTP (for embedding in a web server)

## Maven Coordinates

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-middleware-mcp</artifactId>
   <version>0.1.0-SNAPSHOT</version>
</dependency>
```

You will also need the QQQ BOM for transitive dependency management:

```xml
<dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>com.kingsrook.qqq</groupId>
         <artifactId>qqq-bom-pom</artifactId>
         <version>0.40.0-SNAPSHOT</version>
         <type>pom</type>
         <scope>import</scope>
      </dependency>
   </dependencies>
</dependencyManagement>
```

## Quick Start

### 1. Build and start the MCP server

```java
QInstance qInstance = buildYourQInstance(); // your QQQ metadata

QMCPMetaData mcpMetaData = new QMCPMetaData()
    .withSessionProvider(new FixedUserMCPSessionProvider(mySession))
    .withExposedTableNames(Set.of("order", "customer"))
    .withServerName("my-app");

QMCPServer.startStdio(qInstance, mcpMetaData);
```

### 2. Configure Claude Desktop

Add the server to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "my-app": {
      "command": "java",
      "args": ["-jar", "/path/to/my-app.jar"]
    }
  }
}
```

Claude will now be able to list your tables, query records, and execute processes.

## Configuration

### QMCPMetaData

| Property | Type | Default | Description |
|---|---|---|---|
| `exposedTableNames` | `Set<String>` | `null` (all) | Tables to expose. `null` means all non-hidden tables. |
| `exposedProcessNames` | `Set<String>` | `null` (none) | Processes to expose. `null` means none (safe default). |
| `defaultTableOperations` | `Set<MCPOperation>` | `QUERY, COUNT, GET` | Operations allowed globally (read-only by default). |
| `sessionProvider` | `MCPSessionProviderInterface` | *required* | How to authenticate MCP connections. |
| `defaultQueryLimit` | `int` | `100` | Default records per query page. |
| `maxQueryLimit` | `int` | `1000` | Hard ceiling on records per query. |
| `serverName` | `String` | `"qqq-mcp-server"` | Server identity in the MCP handshake. |
| `serverVersion` | `String` | `"1.0.0"` | Server version in the MCP handshake. |

### Operations

The `MCPOperation` enum controls what operations are available:

| Operation | Description | Default |
|---|---|---|
| `QUERY` | Search/filter records with criteria, ordering, pagination | Enabled |
| `COUNT` | Count records matching a filter | Enabled |
| `GET` | Retrieve a single record by primary key | Enabled |
| `INSERT` | Create a new record | **Disabled** |
| `UPDATE` | Update an existing record | **Disabled** |
| `DELETE` | Delete a record | **Disabled** |

To enable write operations:

```java
QMCPMetaData mcpMetaData = new QMCPMetaData()
    .withDefaultTableOperations(EnumSet.allOf(MCPOperation.class))
    // ...
```

### Per-Table Configuration

Use `MCPTableMetaData` as supplemental metadata on a `QTableMetaData` to customize per-table behavior:

```java
table.withSupplementalMetaData(new MCPTableMetaData()
    .withAiDescription("Customer orders with line items and shipping details")
    .withAllowedOperations(EnumSet.of(MCPOperation.QUERY, MCPOperation.COUNT, MCPOperation.GET))
    .withExposedFieldNames(Set.of("id", "customerName", "orderDate", "total"))
    .withExcluded(false));
```

| Property | Type | Description |
|---|---|---|
| `aiDescription` | `String` | Natural-language description for the AI |
| `allowedOperations` | `Set<MCPOperation>` | Override default operations for this table |
| `exposedFieldNames` | `Set<String>` | Fields to expose (`null` = all non-hidden) |
| `excluded` | `boolean` | If `true`, table is hidden from MCP entirely |

### Session Providers

| Provider | Use Case |
|---|---|
| `FixedUserMCPSessionProvider` | Local/trusted STDIO (e.g., Claude Desktop). Uses a pre-authenticated `QSession`. |
| `AnonymousMCPSessionProvider` | Read-only public data. Returns an empty session; QQQ permissions govern access. |

Implement `MCPSessionProviderInterface` for custom authentication:

```java
public class MySessionProvider implements MCPSessionProviderInterface
{
   @Override
   public QSession createSession(McpSyncServerExchange exchange) throws QException
   {
      // Extract credentials from exchange, authenticate, return a QSession
   }
}
```

## What Gets Exposed

### Tools

| Tool | Description |
|---|---|
| `list_tables` | Enumerate all exposed tables |
| `describe_table` | Describe a table's fields, types, and relationships |
| `query_{tableName}` | Search/filter records |
| `count_{tableName}` | Count matching records |
| `get_{tableName}` | Retrieve a record by primary key |
| `insert_{tableName}` | Insert a new record (opt-in) |
| `update_{tableName}` | Update a record (opt-in) |
| `delete_{tableName}` | Delete a record (opt-in) |
| `run_process_{name}` | Execute a QQQ process (opt-in) |

### Resources

| URI | Description |
|---|---|
| `qqq://tables` | JSON list of all exposed tables |
| `qqq://table/{tableName}/schema` | Field definitions for a table |
| `qqq://table/{tableName}/record/{id}` | URI template for record discovery |

### Prompts

| Prompt | Description |
|---|---|
| `explore_table` | Guides the AI to explore a table: schema, sample records, count, and summary |
| `summarize_data` | Asks the AI to describe data in a table with an optional filter |

## Transport Modes

### STDIO (Claude Desktop)

```java
QMCPServer.startStdio(qInstance, mcpMetaData);
```

Blocks the main thread; intended for `mcpServers` configuration in Claude Desktop.

### Streamable HTTP (Embedded)

```java
McpStreamableServerTransportProvider transport = /* your HTTP transport */;
McpSyncServer server = QMCPServer.build(qInstance, mcpMetaData, transport);
```

For embedding the MCP server in an existing web application.

### To add to an existing QJavalinApplication server:

```java
QSession mySession = new QSystemUserSession(); // Note - this bypasses all permissions, so likely isn't what you want!
// mySession.setUser(new QUser().withFullName("Claude").withIdReference("claude"));

QMCPMetaData mcpMetaData = new QMCPMetaData()
   .withServerName("coldtrack-live")
   .withServerVersion("0.0.0")
   .withSessionProvider(new FixedUserMCPSessionProvider(mySession))
   // Note - do you really want all of these write permissions turned on ??
   .withDefaultTableOperations(Set.of(MCPOperation.QUERY, MCPOperation.COUNT, MCPOperation.GET, MCPOperation.INSERT, MCPOperation.UPDATE, MCPOperation.DELETE));

HttpServletStreamableServerTransportProvider transport =
   HttpServletStreamableServerTransportProvider.builder()
      .objectMapper(new ObjectMapper().findAndRegisterModules())
      .mcpEndpoint("/mcp")
      .build();

McpSyncServer mcpServer = QMCPServer.build(qInstance, mcpMetaData, transport);

// 
QApplicationJavalinServer server = new QApplicationJavalinServer... // assuming you already have this
server.setJavalinConfigurationCustomizer(javalinConfiguration ->
{
   config.jetty.modifyServletContextHandler(ctx ->
   {
      ctx.addServlet(new ServletHolder("mcp", transport), "/mcp/*");
   });
});

```

## Related Projects

- [QQQ Framework](https://github.com/Kingsrook/qqq) — Low-code Application Framework for Engineers
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) — Official Model Context Protocol SDK for Java

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
