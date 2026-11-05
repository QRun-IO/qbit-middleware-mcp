/*
 * Copyright 2025 Kingsrook, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.middlewaremcp;


import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.middlewaremcp.model.MCPOperation;
import com.kingsrook.qbits.middlewaremcp.session.HttpPrincipalMCPSessionProvider;
import com.kingsrook.qbits.middlewaremcp.transport.McpBearerAuthenticationFilter;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import io.javalin.Javalin;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The Streamable HTTP transport as a host mounts it, driven by a REAL MCP client
 ** (the SDK's own client over HTTP): every request carries its own bearer token,
 ** the application's authentication module turns it into that caller's QSession,
 ** and the tools run under that session's table permissions and tenant lock.
 ** Read-only is the default; writes exist only when enabled, and then still only
 ** under the caller's real permissions.
 *******************************************************************************/
class McpHttpSecurityTest
{
   private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

   private QInstance                 instance;
   private Javalin                   readOnlyServer;
   private Javalin                   writableServer;
   private McpSyncServer             readOnlyMcp;
   private McpSyncServer             writableMcp;
   private int                       readOnlyPort;
   private int                       writablePort;



   /*******************************************************************************
    ** An application with a tenant-locked table, a table the server does not
    ** expose, an exposed table nobody here may read, and two MCP servers.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withName("db").withVendor("h2").withHostName("mem")
         .withDatabaseName("mcp_" + UUID.randomUUID().toString().replace("-", "")).withUsername("sa");
      instance = new QInstance();
      instance.setEnforcePermissionsInCoreActions(true);
      instance.addBackend(backend);
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("tokens").withType(QAuthenticationType.CUSTOM)
         .withModuleCodeReference(new QCodeReference(TokenAuthenticationModule.class)));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("tenant"));
      instance.addTable(table("customer", true));
      instance.addTable(table("vault", false));
      instance.addTable(table("secret", false));
      new com.kingsrook.qqq.backend.core.instances.QInstanceValidator().validate(instance);

      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         for(String name : List.of("customer", "vault", "secret"))
         {
            statement.execute("CREATE TABLE " + name + " (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), tenant_id INT)");
         }
         statement.execute("INSERT INTO customer (name, tenant_id) VALUES ('Acme', 1), ('Bolt', 1), ('Cog', 2)");
         statement.execute("INSERT INTO vault (name, tenant_id) VALUES ('V1', 1)");
         statement.execute("INSERT INTO secret (name, tenant_id) VALUES ('S1', 1)");
      }

      Set<String> exposed = Set.of("customer", "vault");
      readOnlyMcp = mcp(new QMCPMetaData().withServerName("test-readonly").withSessionProvider(new HttpPrincipalMCPSessionProvider()).withExposedTableNames(exposed));
      readOnlyServer = serve(readOnlyMcp);
      readOnlyPort = readOnlyServer.port();
      writableMcp = mcp(new QMCPMetaData().withServerName("test-writable").withSessionProvider(new HttpPrincipalMCPSessionProvider()).withExposedTableNames(exposed)
         .withDefaultTableOperations(EnumSet.of(MCPOperation.QUERY, MCPOperation.COUNT, MCPOperation.GET, MCPOperation.INSERT)));
      writableServer = serve(writableMcp);
      writablePort = writableServer.port();
   }



   /*******************************************************************************
    ** Stop everything.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      readOnlyServer.stop();
      writableServer.stop();
      readOnlyMcp.close();
      writableMcp.close();
   }



   /*******************************************************************************
    ** Without a valid bearer token nothing reaches the MCP server: not initialize,
    ** not a cookie, not a basic credential, not a made-up token.
    *******************************************************************************/
   @Test
   void testUnauthenticatedRequestsAreRefused() throws Exception
   {
      String initialize = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}";
      for(String authorization : new String[] { null, "", "Bearer", "Bearer ", "Basic YWxpY2U6cHc=", "Bearer not-a-token", "bearer alice-token" })
      {
         HttpRequest.Builder builder = HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + readOnlyPort + "/mcp"))
            .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream").header("Cookie", "sessionId=alice-token")
            .POST(HttpRequest.BodyPublishers.ofString(initialize));
         if(authorization != null)
         {
            builder.header("Authorization", authorization);
         }
         HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
         assertThat(response.statusCode()).as("authorization=" + authorization).isEqualTo(401);
         assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
         assertThat(response.body()).doesNotContain("serverInfo");
      }
   }



   /*******************************************************************************
    ** initialize, list, read and query as one tenant's user: only what that
    ** caller's permissions and tenant allow.
    *******************************************************************************/
   @Test
   void testAliceSeesOnlyHerPermittedTenantData() throws Exception
   {
      try(McpSyncClient client = client(readOnlyPort, "alice-token"))
      {
         McpSchema.InitializeResult init = client.initialize();
         assertThat(init.serverInfo().name()).isEqualTo("test-readonly");

         List<String> tools = client.listTools().tools().stream().map(McpSchema.Tool::name).toList();
         assertThat(tools).contains("list_tables", "describe_table", "query_customer", "count_customer", "get_customer");
         assertThat(tools).noneMatch(name -> name.startsWith("insert_") || name.startsWith("update_") || name.startsWith("delete_") || name.startsWith("run_process_"));
         assertThat(tools).doesNotContain("query_secret");
         assertThat(client.listResources().resources().stream().map(McpSchema.Resource::uri)).contains("qqq://tables");
         assertThat(client.listPrompts().prompts().stream().map(McpSchema.Prompt::name)).contains("explore_table", "summarize_data");

         // tables: customer only (vault is exposed but alice cannot read it; secret is not exposed)
         String tableList = text(client.callTool(new McpSchema.CallToolRequest("list_tables", Map.of())));
         assertThat(tableList).contains("customer").doesNotContain("vault").doesNotContain("secret");
         String resourceList = ((McpSchema.TextResourceContents) client.readResource(new McpSchema.ReadResourceRequest("qqq://tables")).contents().get(0)).text();
         assertThat(resourceList).contains("customer").doesNotContain("vault").doesNotContain("secret");
         String schema = ((McpSchema.TextResourceContents) client.readResource(new McpSchema.ReadResourceRequest("qqq://table/customer/schema")).contents().get(0)).text();
         assertThat(schema).contains("name");
         assertThat(text(client.callTool(new McpSchema.CallToolRequest("describe_table", Map.of("tableName", "customer"))))).contains("tenantId");

         // not exposed / not visible: reported exactly like a table that does not exist
         String missing = text(client.callTool(new McpSchema.CallToolRequest("describe_table", Map.of("tableName", "no_such_table"))));
         assertThat(text(client.callTool(new McpSchema.CallToolRequest("describe_table", Map.of("tableName", "secret"))))).isEqualTo(missing.replace("no_such_table", "secret"));
         assertThat(text(client.callTool(new McpSchema.CallToolRequest("describe_table", Map.of("tableName", "vault"))))).isEqualTo(missing.replace("no_such_table", "vault"));
         String vaultSchema = ((McpSchema.TextResourceContents) client.readResource(new McpSchema.ReadResourceRequest("qqq://table/vault/schema")).contents().get(0)).text();
         assertThat(vaultSchema).contains("not found");

         // data: tenant 1 only
         String query = text(client.callTool(new McpSchema.CallToolRequest("query_customer", Map.of())));
         assertThat(query).contains("Acme").contains("Bolt").doesNotContain("Cog");
         assertThat(text(client.callTool(new McpSchema.CallToolRequest("count_customer", Map.of())))).contains("2");

         // the other tenant's record, by id, is not available
         McpSchema.CallToolResult other = client.callTool(new McpSchema.CallToolRequest("get_customer", Map.of("id", 3)));
         assertThat(text(other)).doesNotContain("Cog");

         // an exposed table she cannot read: refused with the real permission error
         McpSchema.CallToolResult denied = client.callTool(new McpSchema.CallToolRequest("query_vault", Map.of()));
         assertThat(denied.isError()).isTrue();
         assertThat(text(denied)).containsIgnoringCase("permission").doesNotContain("V1");
      }
   }



   /*******************************************************************************
    ** The same server, another caller: each request is its own caller, so another
    ** tenant's user sees only their own rows, and a caller with no permissions is
    ** refused everything.
    *******************************************************************************/
   @Test
   void testOtherActorsAreSeparate() throws Exception
   {
      try(McpSyncClient bob = client(readOnlyPort, "bob-token"); McpSyncClient eve = client(readOnlyPort, "eve-token"); McpSyncClient alice = client(readOnlyPort, "alice-token"))
      {
         bob.initialize();
         eve.initialize();
         alice.initialize();

         String bobRows = text(bob.callTool(new McpSchema.CallToolRequest("query_customer", Map.of())));
         assertThat(bobRows).contains("Cog").doesNotContain("Acme").doesNotContain("Bolt");
         String aliceRows = text(alice.callTool(new McpSchema.CallToolRequest("query_customer", Map.of())));
         assertThat(aliceRows).contains("Acme").doesNotContain("Cog");

         McpSchema.CallToolResult eveQuery = eve.callTool(new McpSchema.CallToolRequest("query_customer", Map.of()));
         assertThat(eveQuery.isError()).isTrue();
         assertThat(text(eveQuery)).doesNotContain("Acme").doesNotContain("Cog");
         assertThat(text(eve.callTool(new McpSchema.CallToolRequest("list_tables", Map.of())))).doesNotContain("customer");
      }
   }



   /*******************************************************************************
    ** Read-only by default: no write tools exist, and a write call is unknown.  With
    ** writes enabled they still run only under the caller's own permission and
    ** tenant lock.
    *******************************************************************************/
   @Test
   void testMutationsNeedBothOptInAndPermission() throws Exception
   {
      try(McpSyncClient writer = client(readOnlyPort, "writer-token"))
      {
         writer.initialize();
         assertThatThrownBy(() -> writer.callTool(new McpSchema.CallToolRequest("insert_customer", Map.of("record", Map.of("name", "X", "tenantId", 1))))).isInstanceOf(RuntimeException.class);
      }

      try(McpSyncClient writer = client(writablePort, "writer-token"); McpSyncClient alice = client(writablePort, "alice-token"))
      {
         writer.initialize();
         alice.initialize();
         assertThat(writer.listTools().tools().stream().map(McpSchema.Tool::name)).contains("insert_customer").doesNotContain("update_customer", "delete_customer");

         McpSchema.CallToolResult inserted = writer.callTool(new McpSchema.CallToolRequest("insert_customer", Map.of("record", Map.of("name", "Dyna", "tenantId", 1))));
         assertThat(inserted.isError()).as(text(inserted)).isFalse();
         assertThat(text(inserted)).as("insert result").doesNotContain("rror");
         assertThat(text(writer.callTool(new McpSchema.CallToolRequest("query_customer", Map.of())))).contains("Dyna");

         // the tenant lock applies to the write itself
         McpSchema.CallToolResult crossTenant = writer.callTool(new McpSchema.CallToolRequest("insert_customer", Map.of("record", Map.of("name", "Sneaky", "tenantId", 2))));
         assertThat(text(crossTenant)).doesNotContain("\"id\" : 5");
         try(McpSyncClient bob = client(writablePort, "bob-token"))
         {
            bob.initialize();
            assertThat(text(bob.callTool(new McpSchema.CallToolRequest("query_customer", Map.of())))).doesNotContain("Sneaky").doesNotContain("Dyna");
         }

         // alice has no insert permission: refused, nothing written
         McpSchema.CallToolResult aliceInsert = alice.callTool(new McpSchema.CallToolRequest("insert_customer", Map.of("record", Map.of("name", "Nope", "tenantId", 1))));
         assertThat(aliceInsert.isError()).isTrue();
         assertThat(text(alice.callTool(new McpSchema.CallToolRequest("query_customer", Map.of())))).doesNotContain("Nope").doesNotContain("Sneaky");
      }
   }



   /*******************************************************************************
    ** The server can never run as a fixed or system identity over HTTP, and the HTTP
    ** transport will not start without authentication and enforcement.
    *******************************************************************************/
   @Test
   void testNoFixedOrSystemIdentity() throws Exception
   {
      HttpPrincipalMCPSessionProvider provider = new HttpPrincipalMCPSessionProvider();
      assertThatThrownBy(() -> provider.createSession(null)).isInstanceOf(QAuthenticationException.class);

      QInstance noEnforcement = new QInstance();
      noEnforcement.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("tokens").withType(QAuthenticationType.CUSTOM)
         .withModuleCodeReference(new QCodeReference(TokenAuthenticationModule.class)));
      assertThatThrownBy(() -> new McpBearerAuthenticationFilter(noEnforcement)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("enforcement");
      assertThatThrownBy(() -> new McpBearerAuthenticationFilter(new QInstance())).isInstanceOf(IllegalArgumentException.class);

      // a system session is never accepted as a principal, even from a token
      try(McpSyncClient system = client(readOnlyPort, "system-token"))
      {
         assertThatThrownBy(system::initialize).isInstanceOf(RuntimeException.class);
      }
   }



   /*******************************************************************************
    ** The text of a tool result.
    *******************************************************************************/
   private static String text(McpSchema.CallToolResult result)
   {
      return ((McpSchema.TextContent) result.content().get(0)).text();
   }



   /*******************************************************************************
    ** A real MCP client over HTTP with its own bearer token.
    *******************************************************************************/
   private static McpSyncClient client(int port, String token)
   {
      HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp")
         .customizeRequest(request -> request.header("Authorization", "Bearer " + token)).build();
      return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(20)).initializationTimeout(Duration.ofSeconds(20)).build();
   }



   /*******************************************************************************
    ** A table; the first is tenant-locked.
    *******************************************************************************/
   private static QTableMetaData table(String name, boolean tenantLocked)
   {
      QTableMetaData table = new QTableMetaData().withName(name).withBackendName("db").withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(name))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS).withPermissionBaseName(name))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id").withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withBackendName("name").withMaxLength(100))
         .withField(new QFieldMetaData("tenantId", QFieldType.INTEGER).withBackendName("tenant_id"));
      table.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("tenant").withFieldName("tenantId"));
      return table;
   }



   /*******************************************************************************
    ** An MCP server with the metadata, on the streamable servlet transport that
    ** extracts the authenticated session of each request.
    *******************************************************************************/
   private final java.util.Map<McpSyncServer, HttpServletStreamableServerTransportProvider> transports = new java.util.HashMap<>();



   /*******************************************************************************
    ** Build one.
    *******************************************************************************/
   private McpSyncServer mcp(QMCPMetaData metaData)
   {
      HttpServletStreamableServerTransportProvider transport = HttpServletStreamableServerTransportProvider.builder()
         .objectMapper(MAPPER).mcpEndpoint("/mcp").contextExtractor(McpBearerAuthenticationFilter.extractor()).build();
      McpSyncServer server = QMCPServer.build(instance, metaData, transport);
      transports.put(server, transport);
      return server;
   }



   /*******************************************************************************
    ** Mount the transport, behind the authentication filter, in a Javalin server.
    *******************************************************************************/
   private Javalin serve(McpSyncServer server)
   {
      HttpServletStreamableServerTransportProvider transport = transports.get(server);
      return Javalin.create(config -> config.jetty.modifyServletContextHandler(context ->
      {
         context.addFilter(new FilterHolder(new McpBearerAuthenticationFilter(instance)), "/mcp/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
         context.addServlet(new ServletHolder("mcp", transport), "/mcp/*");
      })).start(0);
   }



   /*******************************************************************************
    ** Tokens to synthetic identities: the application's authentication module.
    *******************************************************************************/
   public static class TokenAuthenticationModule implements QAuthenticationModuleInterface
   {
      /***************************************************************************
       ** Resolve the token (a real module would verify it with its provider).
       ***************************************************************************/
      @Override
      public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
      {
         String token = context.get("accessToken");
         if(token == null)
         {
            throw new QAuthenticationException("no token");
         }
         QSession session = switch(token)
         {
            case "alice-token" -> identity("alice", 1, "customer.read");
            case "bob-token" -> identity("bob", 2, "customer.read");
            case "eve-token" -> identity("eve", 1);
            case "writer-token" -> identity("writer", 1, "customer.read", "customer.write");
            case "system-token" -> new QSystemUserSession();
            default -> throw new QAuthenticationException("unknown token");
         };
         return session;
      }



      /***************************************************************************
       ** A synthetic user.
       ***************************************************************************/
      private static QSession identity(String user, int tenant, String... permissions)
      {
         QSession session = new QSession().withUser(new QUser().withIdReference(user).withFullName(user)).withSecurityKeyValue("tenant", tenant);
         session.setUuid(UUID.randomUUID().toString());
         session.setIdReference(session.getUuid());
         session.setPermissions(Set.of(permissions));
         return session;
      }



      /***************************************************************************
       ** Always valid for this test module.
       ***************************************************************************/
      @Override
      public boolean isSessionValid(QInstance instance, QSession session)
      {
         return true;
      }
   }
}
