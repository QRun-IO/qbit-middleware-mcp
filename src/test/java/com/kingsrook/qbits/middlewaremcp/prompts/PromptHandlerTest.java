/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2025-2025.  Kingsrook, LLC
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


import java.util.Map;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for ExploreTablePromptHandler and SummarizeDataPromptHandler.
 *******************************************************************************/
class PromptHandlerTest
{

   /*******************************************************************************
    ** Test explore_table prompt with a table name.
    *******************************************************************************/
   @Test
   void testExploreTableWithTableName()
   {
      ExploreTablePromptHandler handler = new ExploreTablePromptHandler();
      McpSchema.GetPromptResult result = handler.handle(null,
         new McpSchema.GetPromptRequest("explore_table", Map.of("tableName", "order")));

      assertThat(result).isNotNull();
      assertThat(result.description()).contains("order");
      assertThat(result.messages()).hasSize(1);

      String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
      assertThat(text).contains("order");
      assertThat(text).contains("describe_table");
      assertThat(text).contains("query_order");
      assertThat(text).contains("count_order");
   }



   /*******************************************************************************
    ** Test explore_table prompt with a table name and goal.
    *******************************************************************************/
   @Test
   void testExploreTableWithGoal()
   {
      ExploreTablePromptHandler handler = new ExploreTablePromptHandler();
      McpSchema.GetPromptResult result = handler.handle(null,
         new McpSchema.GetPromptRequest("explore_table", Map.of("tableName", "customer", "goal", "Find top spenders")));

      String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
      assertThat(text).contains("customer");
      assertThat(text).contains("Find top spenders");
   }



   /*******************************************************************************
    ** Test explore_table prompt without arguments uses placeholder.
    *******************************************************************************/
   @Test
   void testExploreTableWithoutArguments()
   {
      ExploreTablePromptHandler handler = new ExploreTablePromptHandler();
      McpSchema.GetPromptResult result = handler.handle(null,
         new McpSchema.GetPromptRequest("explore_table", null));

      String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
      assertThat(text).contains("<tableName>");
   }



   /*******************************************************************************
    ** Test summarize_data prompt with a table name.
    *******************************************************************************/
   @Test
   void testSummarizeDataWithTableName()
   {
      SummarizeDataPromptHandler handler = new SummarizeDataPromptHandler();
      McpSchema.GetPromptResult result = handler.handle(null,
         new McpSchema.GetPromptRequest("summarize_data", Map.of("tableName", "invoice")));

      assertThat(result).isNotNull();
      assertThat(result.description()).contains("invoice");
      assertThat(result.messages()).hasSize(1);

      String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
      assertThat(text).contains("invoice");
      assertThat(text).contains("describe_table");
      assertThat(text).contains("count_invoice");
      assertThat(text).contains("query_invoice");
   }



   /*******************************************************************************
    ** Test summarize_data prompt with a filter.
    *******************************************************************************/
   @Test
   void testSummarizeDataWithFilter()
   {
      SummarizeDataPromptHandler handler = new SummarizeDataPromptHandler();
      String filterJson = "{\"fieldName\": \"status\", \"operator\": \"EQUALS\", \"values\": [\"active\"]}";

      McpSchema.GetPromptResult result = handler.handle(null,
         new McpSchema.GetPromptRequest("summarize_data", Map.of("tableName", "order", "filter", filterJson)));

      String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
      assertThat(text).contains("order");
      assertThat(text).contains("with the provided filter");
      assertThat(text).contains(filterJson);
   }



   /*******************************************************************************
    ** Test summarize_data prompt without arguments uses placeholder.
    *******************************************************************************/
   @Test
   void testSummarizeDataWithoutArguments()
   {
      SummarizeDataPromptHandler handler = new SummarizeDataPromptHandler();
      McpSchema.GetPromptResult result = handler.handle(null,
         new McpSchema.GetPromptRequest("summarize_data", null));

      String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
      assertThat(text).contains("<tableName>");
   }

}
