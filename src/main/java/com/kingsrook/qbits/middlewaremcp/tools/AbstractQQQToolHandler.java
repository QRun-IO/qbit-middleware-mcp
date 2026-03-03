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

package com.kingsrook.qbits.middlewaremcp.tools;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;


/*******************************************************************************
 ** Base class for all QQQ MCP tool handlers.
 **
 ** Provides the execute template method that:
 ** 1. Creates QSession via sessionProvider
 ** 2. Initializes QContext
 ** 3. Calls doExecute()
 ** 4. Clears QContext in a finally block
 ** 5. Serializes result to CallToolResult
 *******************************************************************************/
public abstract class AbstractQQQToolHandler
{
   private static final QLogger LOG = QLogger.getLogger(AbstractQQQToolHandler.class);

   protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

   protected final QInstance                 qInstance;
   protected final QMCPMetaData              mcpMetaData;
   protected final MCPSessionProviderInterface sessionProvider;



   /*******************************************************************************
    **
    *******************************************************************************/
   protected AbstractQQQToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider)
   {
      this.qInstance       = qInstance;
      this.mcpMetaData     = mcpMetaData;
      this.sessionProvider = sessionProvider;
   }



   /*******************************************************************************
    ** Template method — subclasses implement doExecute().
    *******************************************************************************/
   public final McpSchema.CallToolResult execute(McpSyncServerExchange exchange, Map<String, Object> arguments)
   {
      try
      {
         QContext.init(qInstance, sessionProvider.createSession(exchange));
         try
         {
            Object result = doExecute(arguments);
            return toTextResult(result);
         }
         catch(QPermissionDeniedException e)
         {
            return toErrorResult("Permission denied: " + e.getMessage());
         }
         catch(QException e)
         {
            LOG.warn("Error in MCP tool handler", e);
            return toErrorResult("Error: " + e.getMessage());
         }
         catch(Exception e)
         {
            LOG.error("Unexpected error in MCP tool handler", e);
            return toErrorResult("Unexpected error: " + e.getMessage());
         }
         finally
         {
            QContext.clear();
         }
      }
      catch(QException e)
      {
         return toErrorResult("Session error: " + e.getMessage());
      }
   }



   /*******************************************************************************
    ** Subclasses implement this method to do the actual work.
    *******************************************************************************/
   protected abstract Object doExecute(Map<String, Object> arguments) throws QException;



   /*******************************************************************************
    ** Convert a result object to a successful CallToolResult.
    *******************************************************************************/
   protected McpSchema.CallToolResult toTextResult(Object result)
   {
      try
      {
         String text = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
         return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
      }
      catch(Exception e)
      {
         return toErrorResult("Failed to serialize result: " + e.getMessage());
      }
   }



   /*******************************************************************************
    ** Create an error CallToolResult.
    *******************************************************************************/
   protected McpSchema.CallToolResult toErrorResult(String message)
   {
      return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(message)), true);
   }



   /*******************************************************************************
    ** Convert a QRecord to a Map suitable for JSON serialization.
    ** Includes display values as fieldName__display when they differ from raw values.
    *******************************************************************************/
   protected Map<String, Object> recordToMap(QRecord record)
   {
      Map<String, Object> map = new LinkedHashMap<>();

      if(record.getValues() != null)
      {
         for(Map.Entry<String, java.io.Serializable> entry : record.getValues().entrySet())
         {
            map.put(entry.getKey(), entry.getValue());

            // Include display value if it differs from raw value
            if(record.getDisplayValues() != null)
            {
               String displayValue = record.getDisplayValues().get(entry.getKey());
               if(displayValue != null)
               {
                  Object rawValue = entry.getValue();
                  if(!displayValue.equals(rawValue == null ? null : String.valueOf(rawValue)))
                  {
                     map.put(entry.getKey() + "__display", displayValue);
                  }
               }
            }
         }
      }

      return map;
   }



   /*******************************************************************************
    ** Convert a list of QRecords to a list of Maps.
    *******************************************************************************/
   protected List<Map<String, Object>> recordsToList(List<QRecord> records)
   {
      List<Map<String, Object>> list = new ArrayList<>();
      if(records != null)
      {
         for(QRecord record : records)
         {
            list.add(recordToMap(record));
         }
      }
      return list;
   }



   /*******************************************************************************
    ** Helper to get a string argument with null safety.
    *******************************************************************************/
   protected String getStringArg(Map<String, Object> arguments, String key)
   {
      Object value = arguments.get(key);
      return value == null ? null : String.valueOf(value);
   }



   /*******************************************************************************
    ** Helper to get an integer argument with a default value.
    *******************************************************************************/
   protected int getIntArg(Map<String, Object> arguments, String key, int defaultValue)
   {
      Object value = arguments.get(key);
      if(value == null)
      {
         return defaultValue;
      }
      if(value instanceof Number)
      {
         return ((Number) value).intValue();
      }
      try
      {
         return Integer.parseInt(String.valueOf(value));
      }
      catch(NumberFormatException e)
      {
         return defaultValue;
      }
   }

}
