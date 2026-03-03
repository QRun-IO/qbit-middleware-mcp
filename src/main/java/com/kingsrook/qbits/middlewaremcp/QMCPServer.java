/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.middlewaremcp.model.MCPOperation;
import com.kingsrook.qbits.middlewaremcp.model.MCPTableMetaData;
import com.kingsrook.qbits.middlewaremcp.prompts.ExploreTablePromptHandler;
import com.kingsrook.qbits.middlewaremcp.prompts.SummarizeDataPromptHandler;
import com.kingsrook.qbits.middlewaremcp.resources.RecordResourceHandler;
import com.kingsrook.qbits.middlewaremcp.resources.TableListResourceHandler;
import com.kingsrook.qbits.middlewaremcp.resources.TableSchemaResourceHandler;
import com.kingsrook.qbits.middlewaremcp.schema.FilterInputSchemaBuilder;
import com.kingsrook.qbits.middlewaremcp.schema.QQQToJsonSchemaConverter;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;
import com.kingsrook.qbits.middlewaremcp.tools.CountToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.DeleteToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.GetToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.InsertToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.ListTablesToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.QueryToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.RunProcessToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.TableSchemaToolHandler;
import com.kingsrook.qbits.middlewaremcp.tools.UpdateToolHandler;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;


/*******************************************************************************
 ** Main entry point for the QQQ MCP Server.
 **
 ** Usage — STDIO mode (Claude Desktop):
 ** <pre>
 **   QMCPMetaData mcpMetaData = new QMCPMetaData()
 **       .withExposedTableNames(Set.of("order", "customer"))
 **       .withSessionProvider(new FixedUserMCPSessionProvider(mySession))
 **       .withServerName("my-app");
 **   QMCPServer.startStdio(qInstance, mcpMetaData);
 ** </pre>
 *******************************************************************************/
public class QMCPServer
{
   private static final QLogger       LOG           = QLogger.getLogger(QMCPServer.class);
   private static final ObjectMapper  OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();



   /*******************************************************************************
    ** Start in STDIO mode. Blocks until stdin closes.
    ** Intended for Claude Desktop "mcpServers" configuration.
    *******************************************************************************/
   public static void startStdio(QInstance qInstance, QMCPMetaData mcpMetaData)
   {
      StdioServerTransportProvider transportProvider = new StdioServerTransportProvider();
      McpSyncServer                server            = build(qInstance, mcpMetaData, transportProvider);
      LOG.info("QQQ MCP Server started in STDIO mode as: " + mcpMetaData.getServerName());

      Runtime.getRuntime().addShutdownHook(new Thread(() ->
      {
         try
         {
            server.close();
         }
         catch(Exception e)
         {
            LOG.warn("Error during MCP server shutdown", e);
         }
      }));

      // Block the main thread until stdin closes or the process is terminated
      try
      {
         Thread.currentThread().join();
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         server.close();
      }
   }



   /*******************************************************************************
    ** Build (but don't start) a McpSyncServer using a session-based transport
    ** (STDIO or SSE). For embedding in another container or for testing.
    *******************************************************************************/
   public static McpSyncServer build(QInstance qInstance, QMCPMetaData mcpMetaData, McpServerTransportProvider transport)
   {
      ServerSpecs specs = buildServerSpecs(qInstance, mcpMetaData);
      return applySpecs(McpServer.sync(transport), mcpMetaData, specs).build();
   }



   /*******************************************************************************
    ** Build (but don't start) a McpSyncServer using a streamable HTTP transport.
    *******************************************************************************/
   public static McpSyncServer build(QInstance qInstance, QMCPMetaData mcpMetaData, McpStreamableServerTransportProvider transport)
   {
      ServerSpecs specs = buildServerSpecs(qInstance, mcpMetaData);
      return applySpecs(McpServer.sync(transport), mcpMetaData, specs).build();
   }



   /*******************************************************************************
    ** Collect all tool, resource, template, and prompt specs from the QInstance.
    *******************************************************************************/
   private static ServerSpecs buildServerSpecs(QInstance qInstance, QMCPMetaData mcpMetaData)
   {
      MCPSessionProviderInterface sessionProvider = mcpMetaData.getSessionProvider();
      if(sessionProvider == null)
      {
         throw new IllegalArgumentException("QMCPMetaData.sessionProvider must be set");
      }

      ServerSpecs specs = new ServerSpecs();

      // --- Generic tools (always registered) ---
      registerGenericTools(qInstance, mcpMetaData, sessionProvider, specs.toolSpecs);

      // --- Table list resource (always registered) ---
      registerTableListResource(qInstance, mcpMetaData, sessionProvider, specs.resourceSpecs);

      // --- Per-table tools and resources ---
      Set<String> exposedTableNames = mcpMetaData.getExposedTableNames();

      for(Map.Entry<String, QTableMetaData> entry : qInstance.getTables().entrySet())
      {
         String         tableName = entry.getKey();
         QTableMetaData table     = entry.getValue();

         if(Boolean.TRUE.equals(table.getIsHidden()))
         {
            continue;
         }

         if(exposedTableNames != null && !exposedTableNames.contains(tableName))
         {
            continue;
         }

         MCPTableMetaData mcpTableMetaData = (MCPTableMetaData) table.getSupplementalMetaData(MCPTableMetaData.TYPE);
         if(mcpTableMetaData != null && mcpTableMetaData.isExcluded())
         {
            continue;
         }

         // Determine effective operations for this table
         Set<MCPOperation> effectiveOperations = (mcpTableMetaData != null && mcpTableMetaData.getAllowedOperations() != null)
            ? mcpTableMetaData.getAllowedOperations()
            : mcpMetaData.getDefaultTableOperations();

         // Register per-table tools
         registerTableTools(qInstance, mcpMetaData, sessionProvider, tableName, table, effectiveOperations, specs.toolSpecs);

         // Register per-table resources
         registerTableResources(qInstance, mcpMetaData, sessionProvider, tableName, specs.resourceSpecs, specs.templateSpecs);
      }

      // --- Process tools ---
      Set<String> exposedProcessNames = mcpMetaData.getExposedProcessNames();
      if(exposedProcessNames != null)
      {
         for(String processName : exposedProcessNames)
         {
            if(qInstance.getProcess(processName) != null)
            {
               registerProcessTool(qInstance, mcpMetaData, sessionProvider, processName, specs.toolSpecs);
            }
         }
      }

      // --- Prompts ---
      registerPrompts(specs.promptSpecs);

      return specs;
   }



   /*******************************************************************************
    ** Apply collected specs to a McpServer builder. Works for any SyncSpecification.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static <S extends McpServer.SyncSpecification<S>> McpServer.SyncSpecification<S> applySpecs(
      McpServer.SyncSpecification<S> builder, QMCPMetaData mcpMetaData, ServerSpecs specs)
   {
      return builder
         .serverInfo(mcpMetaData.getServerName(), mcpMetaData.getServerVersion())
         .capabilities(McpSchema.ServerCapabilities.builder()
            .tools(false)
            .resources(false, false)
            .prompts(false)
            .build())
         .tools(specs.toolSpecs)
         .resources(specs.resourceSpecs)
         .resourceTemplates(specs.templateSpecs)
         .prompts(specs.promptSpecs);
   }



   /*******************************************************************************
    ** Simple container for collected server specs.
    *******************************************************************************/
   private static class ServerSpecs
   {
      List<McpServerFeatures.SyncToolSpecification>     toolSpecs     = new ArrayList<>();
      List<McpServerFeatures.SyncResourceSpecification> resourceSpecs = new ArrayList<>();
      List<McpSchema.ResourceTemplate>                  templateSpecs = new ArrayList<>();
      List<McpServerFeatures.SyncPromptSpecification>   promptSpecs   = new ArrayList<>();
   }



   /*******************************************************************************
    ** Register the generic list_tables and describe_table tools.
    *******************************************************************************/
   private static void registerGenericTools(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, List<McpServerFeatures.SyncToolSpecification> toolSpecs)
   {
      // list_tables
      ListTablesToolHandler listHandler = new ListTablesToolHandler(qInstance, mcpMetaData, sessionProvider);
      toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
         McpSchema.Tool.builder()
            .name("list_tables")
            .description("List all tables accessible to the AI with their names, labels, and descriptions. Call this first to discover what data is available.")
            .inputSchema("{\"type\": \"object\", \"properties\": {}}")
            .build(),
         listHandler::execute
      ));

      // describe_table
      TableSchemaToolHandler describeHandler = new TableSchemaToolHandler(qInstance, mcpMetaData, sessionProvider);
      Map<String, Object>    describeSchema  = new LinkedHashMap<>();
      describeSchema.put("type", "object");
      Map<String, Object> describePropTableName = new LinkedHashMap<>();
      describePropTableName.put("type", "string");
      describePropTableName.put("description", "Name of the table to describe");
      describeSchema.put("properties", Map.of("tableName", describePropTableName));
      describeSchema.put("required", List.of("tableName"));

      toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
         McpSchema.Tool.builder()
            .name("describe_table")
            .description("Describe a table's fields, data types, required fields, and relationships. Call this before querying or inserting data.")
            .inputSchema(toSchemaJson(describeSchema))
            .build(),
         describeHandler::execute
      ));
   }



   /*******************************************************************************
    ** Register query/count/get/insert/update/delete tools for a single table.
    *******************************************************************************/
   private static void registerTableTools(
      QInstance qInstance,
      QMCPMetaData mcpMetaData,
      MCPSessionProviderInterface sessionProvider,
      String tableName,
      QTableMetaData table,
      Set<MCPOperation> effectiveOperations,
      List<McpServerFeatures.SyncToolSpecification> toolSpecs)
   {
      String tableLabel = table.getLabel() != null ? table.getLabel() : tableName;

      // query_{tableName}
      if(effectiveOperations.contains(MCPOperation.QUERY))
      {
         QueryToolHandler queryHandler = new QueryToolHandler(qInstance, mcpMetaData, sessionProvider, tableName);
         toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
               .name("query_" + tableName)
               .description("Search and filter records in the " + tableLabel + " table. Supports criteria, ordering, and pagination.")
               .inputSchema(toSchemaJson(FilterInputSchemaBuilder.buildQueryInputSchema()))
               .build(),
            queryHandler::execute
         ));
      }

      // count_{tableName}
      if(effectiveOperations.contains(MCPOperation.COUNT))
      {
         CountToolHandler    countHandler    = new CountToolHandler(qInstance, mcpMetaData, sessionProvider, tableName);
         Map<String, Object> fullQuerySchema = FilterInputSchemaBuilder.buildQueryInputSchema();
         Map<?, ?>           queryProperties = (Map<?, ?>) fullQuerySchema.get("properties");
         Map<String, Object> countSchema     = new LinkedHashMap<>();
         countSchema.put("type", "object");
         Map<String, Object> countProperties = new LinkedHashMap<>();
         countProperties.put("criteria", queryProperties.get("criteria"));
         countProperties.put("booleanOperator", queryProperties.get("booleanOperator"));
         countSchema.put("properties", countProperties);
         toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
               .name("count_" + tableName)
               .description("Count records matching a filter in the " + tableLabel + " table.")
               .inputSchema(toSchemaJson(countSchema))
               .build(),
            countHandler::execute
         ));
      }

      // get_{tableName}
      if(effectiveOperations.contains(MCPOperation.GET))
      {
         GetToolHandler      getHandler = new GetToolHandler(qInstance, mcpMetaData, sessionProvider, tableName);
         Map<String, Object> getSchema  = new LinkedHashMap<>();
         getSchema.put("type", "object");
         Map<String, Object> getIdProp = new LinkedHashMap<>();
         getIdProp.put("description", "Primary key value of the record to retrieve");
         getSchema.put("properties", Map.of("id", getIdProp));
         getSchema.put("required", List.of("id"));
         toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
               .name("get_" + tableName)
               .description("Retrieve a single " + tableLabel + " record by its primary key.")
               .inputSchema(toSchemaJson(getSchema))
               .build(),
            getHandler::execute
         ));
      }

      // insert_{tableName}
      if(effectiveOperations.contains(MCPOperation.INSERT))
      {
         InsertToolHandler   insertHandler = new InsertToolHandler(qInstance, mcpMetaData, sessionProvider, tableName);
         Map<String, Object> insertSchema  = buildInsertSchema(table, mcpMetaData.getExposedTableNames());
         toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
               .name("insert_" + tableName)
               .description("Insert a new record into the " + tableLabel + " table.")
               .inputSchema(toSchemaJson(insertSchema))
               .build(),
            insertHandler::execute
         ));
      }

      // update_{tableName}
      if(effectiveOperations.contains(MCPOperation.UPDATE))
      {
         UpdateToolHandler   updateHandler = new UpdateToolHandler(qInstance, mcpMetaData, sessionProvider, tableName);
         Map<String, Object> updateSchema  = buildUpdateSchema(table);
         toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
               .name("update_" + tableName)
               .description("Update an existing " + tableLabel + " record by its primary key.")
               .inputSchema(toSchemaJson(updateSchema))
               .build(),
            updateHandler::execute
         ));
      }

      // delete_{tableName}
      if(effectiveOperations.contains(MCPOperation.DELETE))
      {
         DeleteToolHandler   deleteHandler = new DeleteToolHandler(qInstance, mcpMetaData, sessionProvider, tableName);
         Map<String, Object> deleteSchema  = new LinkedHashMap<>();
         deleteSchema.put("type", "object");
         Map<String, Object> deleteIdProp = new LinkedHashMap<>();
         deleteIdProp.put("description", "Primary key value of the record to delete");
         deleteSchema.put("properties", Map.of("id", deleteIdProp));
         deleteSchema.put("required", List.of("id"));
         toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
            McpSchema.Tool.builder()
               .name("delete_" + tableName)
               .description("Delete a " + tableLabel + " record by its primary key. Use with caution.")
               .inputSchema(toSchemaJson(deleteSchema))
               .build(),
            deleteHandler::execute
         ));
      }
   }



   /*******************************************************************************
    ** Register resource (URI-addressable) handlers for a single table.
    ** Schema resources are registered as static resources (exact URI per table).
    ** Record resources are listed as URI templates for discovery only.
    *******************************************************************************/
   private static void registerTableResources(
      QInstance qInstance,
      QMCPMetaData mcpMetaData,
      MCPSessionProviderInterface sessionProvider,
      String tableName,
      List<McpServerFeatures.SyncResourceSpecification> resourceSpecs,
      List<McpSchema.ResourceTemplate> templateSpecs)
   {
      // Schema: register as a static resource with exact URI (table names are known at startup)
      TableSchemaResourceHandler schemaHandler = new TableSchemaResourceHandler(qInstance, mcpMetaData, sessionProvider);
      resourceSpecs.add(new McpServerFeatures.SyncResourceSpecification(
         McpSchema.Resource.builder()
            .uri("qqq://table/" + tableName + "/schema")
            .name("schema-" + tableName)
            .description("Schema (field definitions) for the " + tableName + " table")
            .mimeType("application/json")
            .build(),
         schemaHandler::handle
      ));

      // Record: register as a URI template for discovery; actual record access is via get_{tableName} tool
      templateSpecs.add(new McpSchema.ResourceTemplate(
         "qqq://table/" + tableName + "/record/{id}",
         "record-" + tableName,
         "A single record from the " + tableName + " table by primary key",
         "application/json",
         null
      ));
   }



   /*******************************************************************************
    ** Register the run_process_{processName} tool.
    *******************************************************************************/
   private static void registerProcessTool(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, String processName, List<McpServerFeatures.SyncToolSpecification> toolSpecs)
   {
      RunProcessToolHandler processHandler = new RunProcessToolHandler(qInstance, mcpMetaData, sessionProvider, processName);
      Map<String, Object>   processSchema  = new LinkedHashMap<>();
      processSchema.put("type", "object");
      processSchema.put("description", "Input values for the process (varies by process)");
      processSchema.put("additionalProperties", true);

      toolSpecs.add(new McpServerFeatures.SyncToolSpecification(
         McpSchema.Tool.builder()
            .name("run_process_" + processName)
            .description("Execute the QQQ process: " + processName)
            .inputSchema(toSchemaJson(processSchema))
            .build(),
         processHandler::execute
      ));
   }



   /*******************************************************************************
    ** Register the explore_table and summarize_data prompt templates.
    *******************************************************************************/
   private static void registerPrompts(List<McpServerFeatures.SyncPromptSpecification> promptSpecs)
   {
      ExploreTablePromptHandler exploreHandler = new ExploreTablePromptHandler();
      promptSpecs.add(new McpServerFeatures.SyncPromptSpecification(
         new McpSchema.Prompt(
            "explore_table",
            "Instructs the AI to systematically explore a QQQ table: describe schema, sample records, and summarize findings.",
            List.of(
               new McpSchema.PromptArgument("tableName", "Name of the table to explore", true),
               new McpSchema.PromptArgument("goal", "Optional goal or question to focus the exploration", false)
            )
         ),
         exploreHandler::handle
      ));

      SummarizeDataPromptHandler summarizeHandler = new SummarizeDataPromptHandler();
      promptSpecs.add(new McpServerFeatures.SyncPromptSpecification(
         new McpSchema.Prompt(
            "summarize_data",
            "Asks the AI to summarize and explain data in a table, with an optional filter.",
            List.of(
               new McpSchema.PromptArgument("tableName", "Name of the table to summarize", true),
               new McpSchema.PromptArgument("filter", "Optional JSON filter criteria to apply", false)
            )
         ),
         summarizeHandler::handle
      ));
   }



   /*******************************************************************************
    ** Also register the qqq://tables resource (the table list resource).
    *******************************************************************************/
   public static void registerTableListResource(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, List<McpServerFeatures.SyncResourceSpecification> resourceSpecs)
   {
      TableListResourceHandler listHandler = new TableListResourceHandler(qInstance, mcpMetaData, sessionProvider);
      resourceSpecs.add(new McpServerFeatures.SyncResourceSpecification(
         new McpSchema.Resource(
            TableListResourceHandler.URI,
            "table-list",
            "JSON list of all exposed QQQ tables with their labels and descriptions",
            "application/json",
            null
         ),
         listHandler::handle
      ));
   }



   /*******************************************************************************
    ** Serialize a schema Map to a JSON string for use as MCP tool inputSchema.
    *******************************************************************************/
   private static String toSchemaJson(Map<String, Object> schema)
   {
      try
      {
         return OBJECT_MAPPER.writeValueAsString(schema);
      }
      catch(Exception e)
      {
         LOG.warn("Failed to serialize schema to JSON", e);
         return "{\"type\": \"object\"}";
      }
   }



   /*******************************************************************************
    ** Build the JSON Schema for an insert tool.
    *******************************************************************************/
   private static Map<String, Object> buildInsertSchema(QTableMetaData table, @SuppressWarnings("unused") Set<String> exposedTableNames)
   {
      QQQToJsonSchemaConverter converter   = new QQQToJsonSchemaConverter();
      Map<String, Object>      recordProps = new LinkedHashMap<>();

      for(QFieldMetaData field : table.getFields().values())
      {
         if(!field.getIsHidden() && field.getIsEditable())
         {
            recordProps.put(field.getName(), converter.fieldToJsonSchemaProperty(field));
         }
      }

      Map<String, Object> recordSchema = new LinkedHashMap<>();
      recordSchema.put("type", "object");
      recordSchema.put("properties", recordProps);
      recordSchema.put("description", "Field values for the new record");

      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "object");
      schema.put("properties", Map.of("record", recordSchema));
      schema.put("required", List.of("record"));
      return schema;
   }



   /*******************************************************************************
    ** Build the JSON Schema for an update tool.
    *******************************************************************************/
   private static Map<String, Object> buildUpdateSchema(QTableMetaData table)
   {
      QQQToJsonSchemaConverter converter   = new QQQToJsonSchemaConverter();
      Map<String, Object>      recordProps = new LinkedHashMap<>();

      for(QFieldMetaData field : table.getFields().values())
      {
         if(!field.getIsHidden() && field.getIsEditable())
         {
            recordProps.put(field.getName(), converter.fieldToJsonSchemaProperty(field));
         }
      }

      Map<String, Object> idProp = new LinkedHashMap<>();
      idProp.put("description", "Primary key of the record to update");

      Map<String, Object> recordSchema = new LinkedHashMap<>();
      recordSchema.put("type", "object");
      recordSchema.put("properties", recordProps);
      recordSchema.put("description", "Fields to update (only provided fields will be changed)");

      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "object");
      schema.put("properties", Map.of("id", idProp, "record", recordSchema));
      schema.put("required", List.of("id", "record"));
      return schema;
   }

}
