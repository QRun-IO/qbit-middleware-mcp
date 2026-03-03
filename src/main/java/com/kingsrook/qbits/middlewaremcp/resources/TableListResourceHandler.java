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
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.model.MCPTableMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;


/*******************************************************************************
 ** Resource handler for qqq://tables — returns a JSON list of all exposed tables.
 *******************************************************************************/
public class TableListResourceHandler
{
   private static final QLogger       LOG           = QLogger.getLogger(TableListResourceHandler.class);
   private static final ObjectMapper  OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
   public static final  String        URI           = "qqq://tables";

   private final QInstance                  qInstance;
   private final QMCPMetaData               mcpMetaData;
   private final MCPSessionProviderInterface sessionProvider;



   /*******************************************************************************
    **
    *******************************************************************************/
   public TableListResourceHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider)
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
      try
      {
         QContext.init(qInstance, sessionProvider.createSession(exchange));
         try
         {
            Set<String>               exposedTableNames = mcpMetaData.getExposedTableNames();
            List<Map<String, Object>> tables            = new ArrayList<>();

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

               Map<String, Object> tableInfo = new LinkedHashMap<>();
               tableInfo.put("name", tableName);
               tableInfo.put("label", table.getLabel() != null ? table.getLabel() : tableName);
               if(mcpTableMetaData != null && mcpTableMetaData.getAiDescription() != null)
               {
                  tableInfo.put("description", mcpTableMetaData.getAiDescription());
               }
               tables.add(tableInfo);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tables", tables);
            result.put("count", tables.size());

            String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(URI, "application/json", json)));
         }
         finally
         {
            QContext.clear();
         }
      }
      catch(QException | com.fasterxml.jackson.core.JsonProcessingException e)
      {
         LOG.error("Error handling table list resource", e);
         String errorJson = "{\"error\": \"" + e.getMessage() + "\"}";
         return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(URI, "application/json", errorJson)));
      }
   }

}
