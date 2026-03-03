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

package com.kingsrook.qbits.middlewaremcp.resources;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.model.MCPTableMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;


/*******************************************************************************
 ** Resource handler for qqq://table/{tableName}/schema — returns rich field
 ** metadata for a specific table so the AI can construct valid insert/update payloads.
 *******************************************************************************/
public class TableSchemaResourceHandler
{
   private static final QLogger      LOG           = QLogger.getLogger(TableSchemaResourceHandler.class);
   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
   public static final  String       URI_TEMPLATE  = "qqq://table/{tableName}/schema";

   private final QInstance                  qInstance;
   private final QMCPMetaData               mcpMetaData;
   private final MCPSessionProviderInterface sessionProvider;



   /*******************************************************************************
    **
    *******************************************************************************/
   public TableSchemaResourceHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider)
   {
      this.qInstance       = qInstance;
      this.mcpMetaData     = mcpMetaData;
      this.sessionProvider = sessionProvider;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public McpSchema.ReadResourceResult handle(McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request)
   {
      String uri = request.uri();

      // Extract table name from URI: qqq://table/{tableName}/schema
      String tableName = extractTableName(uri);
      if(tableName == null)
      {
         String errorJson = "{\"error\": \"Could not extract table name from URI: " + uri + "\"}";
         return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", errorJson)));
      }

      try
      {
         QContext.init(qInstance, sessionProvider.createSession(exchange));
         try
         {
            QTableMetaData table = qInstance.getTable(tableName);
            if(table == null)
            {
               String errorJson = "{\"error\": \"Table not found: " + tableName + "\"}";
               return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", errorJson)));
            }

            MCPTableMetaData mcpTableMetaData = (MCPTableMetaData) table.getSupplementalMetaData(MCPTableMetaData.TYPE);
            Set<String>      exposedFields    = mcpTableMetaData != null ? mcpTableMetaData.getExposedFieldNames() : null;

            List<Map<String, Object>> fields = new ArrayList<>();
            for(QFieldMetaData field : table.getFields().values())
            {
               if(field.getIsHidden())
               {
                  continue;
               }
               if(exposedFields != null && !exposedFields.contains(field.getName()))
               {
                  continue;
               }

               Map<String, Object> fieldInfo = new LinkedHashMap<>();
               fieldInfo.put("name", field.getName());
               fieldInfo.put("label", field.getLabel() != null ? field.getLabel() : field.getName());
               fieldInfo.put("type", field.getType() != null ? field.getType().name() : "STRING");
               fieldInfo.put("isRequired", field.getIsRequired());
               fieldInfo.put("isEditable", field.getIsEditable());
               fieldInfo.put("isPrimaryKey", field.getName().equals(table.getPrimaryKeyField()));

               if(field.getDisplayFormat() != null)
               {
                  fieldInfo.put("displayFormat", field.getDisplayFormat());
               }
               if(field.getPossibleValueSourceName() != null)
               {
                  fieldInfo.put("possibleValueSourceName", field.getPossibleValueSourceName());
               }
               if(field.getDefaultValue() != null)
               {
                  fieldInfo.put("defaultValue", String.valueOf(field.getDefaultValue()));
               }
               if(field.getMaxLength() != null)
               {
                  fieldInfo.put("maxLength", field.getMaxLength());
               }

               fields.add(fieldInfo);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tableName", tableName);
            result.put("label", table.getLabel() != null ? table.getLabel() : tableName);
            result.put("primaryKeyField", table.getPrimaryKeyField());
            if(mcpTableMetaData != null && mcpTableMetaData.getAiDescription() != null)
            {
               result.put("description", mcpTableMetaData.getAiDescription());
            }
            result.put("fields", fields);

            String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", json)));
         }
         finally
         {
            QContext.clear();
         }
      }
      catch(QException | com.fasterxml.jackson.core.JsonProcessingException e)
      {
         LOG.error("Error handling table schema resource", e);
         String errorJson = "{\"error\": \"" + e.getMessage() + "\"}";
         return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", errorJson)));
      }
   }



   /*******************************************************************************
    ** Extract table name from URI like "qqq://table/customer/schema"
    *******************************************************************************/
   private String extractTableName(String uri)
   {
      if(uri == null)
      {
         return null;
      }
      // qqq://table/{tableName}/schema
      String prefix = "qqq://table/";
      String suffix = "/schema";
      if(uri.startsWith(prefix) && uri.endsWith(suffix))
      {
         return uri.substring(prefix.length(), uri.length() - suffix.length());
      }
      return null;
   }

}
