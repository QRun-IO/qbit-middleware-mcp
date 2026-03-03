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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;


/*******************************************************************************
 ** Resource handler for qqq://table/{tableName}/record/{id} — retrieves a
 ** single record by primary key as a stable URI resource.
 *******************************************************************************/
public class RecordResourceHandler
{
   private static final QLogger      LOG           = QLogger.getLogger(RecordResourceHandler.class);
   private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
   public static final  String       URI_TEMPLATE  = "qqq://table/{tableName}/record/{id}";

   private final QInstance                  qInstance;
   private final QMCPMetaData               mcpMetaData;
   private final MCPSessionProviderInterface sessionProvider;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordResourceHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider)
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

      String[] parts = extractTableNameAndId(uri);
      if(parts == null)
      {
         String errorJson = "{\"error\": \"Could not extract table name and id from URI: " + uri + "\"}";
         return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", errorJson)));
      }

      String tableName = parts[0];
      String id        = parts[1];

      try
      {
         QContext.init(qInstance, sessionProvider.createSession(exchange));
         try
         {
            GetInput getInput = new GetInput();
            getInput.setTableName(tableName);
            getInput.setPrimaryKey(id);
            getInput.setShouldGenerateDisplayValues(true);
            getInput.setShouldTranslatePossibleValues(true);

            GetOutput getOutput = new GetAction().execute(getInput);
            QRecord   record    = getOutput.getRecord();

            Map<String, Object> result = new LinkedHashMap<>();
            if(record == null)
            {
               result.put("error", "Record not found: " + id);
            }
            else
            {
               // Build record map with display values
               if(record.getValues() != null)
               {
                  for(Map.Entry<String, java.io.Serializable> entry : record.getValues().entrySet())
                  {
                     result.put(entry.getKey(), entry.getValue());
                     if(record.getDisplayValues() != null)
                     {
                        String displayValue = record.getDisplayValues().get(entry.getKey());
                        if(displayValue != null)
                        {
                           Object rawValue = entry.getValue();
                           if(!displayValue.equals(rawValue == null ? null : String.valueOf(rawValue)))
                           {
                              result.put(entry.getKey() + "__display", displayValue);
                           }
                        }
                     }
                  }
               }
            }

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
         LOG.error("Error handling record resource", e);
         String errorJson = "{\"error\": \"" + e.getMessage() + "\"}";
         return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", errorJson)));
      }
   }



   /*******************************************************************************
    ** Extract [tableName, id] from URI like "qqq://table/customer/record/42"
    *******************************************************************************/
   private String[] extractTableNameAndId(String uri)
   {
      if(uri == null)
      {
         return null;
      }
      // qqq://table/{tableName}/record/{id}
      String prefix = "qqq://table/";
      String middle = "/record/";
      if(uri.startsWith(prefix))
      {
         String rest = uri.substring(prefix.length());
         int    idx  = rest.indexOf(middle);
         if(idx > 0)
         {
            String tableName = rest.substring(0, idx);
            String id        = rest.substring(idx + middle.length());
            if(!tableName.isEmpty() && !id.isEmpty())
            {
               return new String[] { tableName, id };
            }
         }
      }
      return null;
   }

}
