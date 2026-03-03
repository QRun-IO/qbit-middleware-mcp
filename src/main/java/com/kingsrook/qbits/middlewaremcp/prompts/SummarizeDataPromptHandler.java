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

package com.kingsrook.qbits.middlewaremcp.prompts;


import java.util.List;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;


/*******************************************************************************
 ** Prompt handler for "summarize_data" — asks the AI to summarize and explain
 ** data in a table, optionally filtered by a JSON criteria expression.
 *******************************************************************************/
public class SummarizeDataPromptHandler
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public McpSchema.GetPromptResult handle(McpSyncServerExchange exchange, McpSchema.GetPromptRequest request)
   {
      String tableName  = null;
      String filterJson = null;

      if(request.arguments() != null)
      {
         tableName  = (String) request.arguments().get("tableName");
         filterJson = (String) request.arguments().get("filter");
      }

      if(tableName == null || tableName.isEmpty())
      {
         tableName = "<tableName>";
      }

      String promptText = buildPromptText(tableName, filterJson);

      return new McpSchema.GetPromptResult(
         "Summarize data in the " + tableName + " table",
         List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(promptText)))
      );
   }



   /*******************************************************************************
    ** Build the prompt text for summarizing table data.
    *******************************************************************************/
   private String buildPromptText(String tableName, String filterJson)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("""
         Please summarize and explain the data in the "%s" table.

         Steps to follow:
         1. Call describe_table with tableName="%s" to understand the schema.
         2. Call count_%s to get the total record count%s.
         3. Call query_%s with limit=20%s to get a representative sample.
         4. Provide a clear summary including:
            - How many records exist
            - What the data represents
            - Key trends, distributions, or notable values
            - Any anomalies or patterns worth highlighting
         """.formatted(
         tableName, tableName,
         tableName, filterJson != null ? " (with the provided filter)" : "",
         tableName, filterJson != null ? " (with the provided filter)" : ""
      ));

      if(filterJson != null && !filterJson.isEmpty())
      {
         sb.append("\nApply this filter to your queries:\n").append(filterJson).append("\n");
      }

      return sb.toString();
   }

}
