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
 ** Prompt handler for "explore_table" — instructs the AI to systematically
 ** explore a QQQ table: describe its schema, sample records, and summarize findings.
 *******************************************************************************/
public class ExploreTablePromptHandler
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public McpSchema.GetPromptResult handle(McpSyncServerExchange exchange, McpSchema.GetPromptRequest request)
   {
      String tableName = null;
      String goal      = "";

      if(request.arguments() != null)
      {
         tableName = (String) request.arguments().get("tableName");
         String goalArg = (String) request.arguments().get("goal");
         if(goalArg != null)
         {
            goal = goalArg;
         }
      }

      if(tableName == null || tableName.isEmpty())
      {
         tableName = "<tableName>";
      }

      String promptText = buildPromptText(tableName, goal);

      return new McpSchema.GetPromptResult(
         "Guided exploration of the " + tableName + " table",
         List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(promptText)))
      );
   }



   /*******************************************************************************
    ** Build the prompt text for exploring a table.
    *******************************************************************************/
   private String buildPromptText(String tableName, String goal)
   {
      return """
         You are helping a user understand the data in the "%s" table.

         Please follow these steps:
         1. Call describe_table with tableName="%s" to understand its schema, fields, and data types.
         2. Call query_%s with a limit of 5 to see a sample of the data.
         3. Call count_%s to see how many total records exist.
         4. Summarize what you find, including:
            - The total record count
            - Key fields and their purposes
            - Notable patterns or data distributions
            - Any relationships to other tables (via possible value sources)

         %s
         """.formatted(
         tableName, tableName, tableName, tableName,
         goal.isEmpty() ? "" : "Goal: " + goal
      );
   }

}
