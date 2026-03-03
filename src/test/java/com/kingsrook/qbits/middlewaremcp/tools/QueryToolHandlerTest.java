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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.TestUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for QueryToolHandler — verifies query and filter functionality.
 *******************************************************************************/
class QueryToolHandlerTest
{
   private QInstance    qInstance;
   private QMCPMetaData mcpMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      qInstance   = TestUtils.defineInstance();
      mcpMetaData = TestUtils.defineMCPMetaData();
      QContext.init(qInstance, new QSession());

      // Insert test records
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Alice").withValue("lastName", "Smith").withValue("age", 30),
         new QRecord().withValue("firstName", "Bob").withValue("lastName", "Jones").withValue("age", 25),
         new QRecord().withValue("firstName", "Carol").withValue("lastName", "Smith").withValue("age", 35)
      ));
      new InsertAction().execute(insertInput);

      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    ** Test that querying all records returns all inserted records.
    *******************************************************************************/
   @Test
   void testQueryAllRecords()
   {
      QueryToolHandler    handler = new QueryToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      assertThat(result.content()).isNotEmpty();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("records");
      assertThat(text).contains("Alice");
      assertThat(text).contains("Bob");
      assertThat(text).contains("Carol");
   }



   /*******************************************************************************
    ** Test filtering by equality operator.
    *******************************************************************************/
   @Test
   void testQueryWithEqualsFilter()
   {
      QueryToolHandler handler = new QueryToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);

      Map<String, Object> arguments = Map.of(
         "criteria", List.of(
            Map.of("fieldName", "lastName", "operator", "EQUALS", "values", List.of("Smith"))
         )
      );

      McpSchema.CallToolResult result = handler.execute(null, arguments);

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("Alice");
      assertThat(text).contains("Carol");
      assertThat(text).doesNotContain("Bob");
   }



   /*******************************************************************************
    ** Test that limit is respected.
    *******************************************************************************/
   @Test
   void testQueryWithLimit()
   {
      QueryToolHandler    handler   = new QueryToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result = handler.execute(null, Map.of("limit", 1));

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      // Should have pageInfo with limit=1
      assertThat(text).contains("\"limit\" : 1");
   }



   /*******************************************************************************
    ** Test building a QQueryFilter from MCP arguments — unit test without DB.
    *******************************************************************************/
   @Test
   void testBuildFilter_equalsOperator()
   {
      Map<String, Object> args = Map.of(
         "criteria", List.of(
            Map.of("fieldName", "status", "operator", "EQUALS", "values", List.of("active"))
         ),
         "booleanOperator", "AND"
      );

      com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter filter = QueryToolHandler.buildFilter(args);

      assertThat(filter.getCriteria()).hasSize(1);
      assertThat(filter.getCriteria().get(0).getFieldName()).isEqualTo("status");
      assertThat(filter.getCriteria().get(0).getOperator()).isEqualTo(com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS);
      assertThat(filter.getCriteria().get(0).getValues()).containsExactly("active");
      assertThat(filter.getBooleanOperator()).isEqualTo(com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter.BooleanOperator.AND);
   }



   /*******************************************************************************
    ** Test building a filter with OR boolean operator.
    *******************************************************************************/
   @Test
   void testBuildFilter_orOperator()
   {
      Map<String, Object> args = Map.of(
         "booleanOperator", "OR",
         "criteria", List.of(
            Map.of("fieldName", "age", "operator", "LESS_THAN", "values", List.of("18")),
            Map.of("fieldName", "age", "operator", "GREATER_THAN", "values", List.of("65"))
         )
      );

      com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter filter = QueryToolHandler.buildFilter(args);
      assertThat(filter.getBooleanOperator()).isEqualTo(com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter.BooleanOperator.OR);
      assertThat(filter.getCriteria()).hasSize(2);
   }



   /*******************************************************************************
    ** Test building a filter with orderBys.
    *******************************************************************************/
   @Test
   void testBuildFilter_withOrderBys()
   {
      Map<String, Object> args = Map.of(
         "orderBys", List.of(
            Map.of("fieldName", "lastName", "isAscending", true),
            Map.of("fieldName", "age", "isAscending", false)
         )
      );

      com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter filter = QueryToolHandler.buildFilter(args);
      assertThat(filter.getOrderBys()).hasSize(2);
      assertThat(filter.getOrderBys().get(0).getFieldName()).isEqualTo("lastName");
      assertThat(filter.getOrderBys().get(0).getIsAscending()).isTrue();
      assertThat(filter.getOrderBys().get(1).getFieldName()).isEqualTo("age");
      assertThat(filter.getOrderBys().get(1).getIsAscending()).isFalse();
   }

}
