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

package com.kingsrook.qbits.middlewaremcp.tools;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.TestUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for CRUD tool handlers: Count, Get, Insert, Update, Delete,
 ** ListTables, and TableSchema.
 *******************************************************************************/
class CrudToolHandlerTest
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
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    ** Test counting all records returns the correct count.
    *******************************************************************************/
   @Test
   void testCountAllRecords()
   {
      CountToolHandler     handler = new CountToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("\"count\" : 3");
   }



   /*******************************************************************************
    ** Test counting with a filter.
    *******************************************************************************/
   @Test
   void testCountWithFilter()
   {
      CountToolHandler handler = new CountToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);

      Map<String, Object> arguments = Map.of(
         "criteria", List.of(
            Map.of("fieldName", "lastName", "operator", "EQUALS", "values", List.of("Smith"))
         )
      );

      McpSchema.CallToolResult result = handler.execute(null, arguments);

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("\"count\" : 2");
   }



   /*******************************************************************************
    ** Test getting a record by primary key.
    *******************************************************************************/
   @Test
   void testGetByPrimaryKey()
   {
      GetToolHandler       handler = new GetToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of("id", 1));

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("Alice");
      assertThat(text).contains("Smith");
   }



   /*******************************************************************************
    ** Test getting a non-existent record returns an error message.
    *******************************************************************************/
   @Test
   void testGetNotFound()
   {
      GetToolHandler       handler = new GetToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of("id", 999));

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("error");
      assertThat(text).contains("not found");
   }



   /*******************************************************************************
    ** Test getting a record without providing an id.
    *******************************************************************************/
   @Test
   void testGetWithoutId()
   {
      GetToolHandler       handler = new GetToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("id is required");
   }



   /*******************************************************************************
    ** Test inserting a new record.
    *******************************************************************************/
   @Test
   void testInsert()
   {
      InsertToolHandler handler = new InsertToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);

      Map<String, Object> arguments = Map.of(
         "record", Map.of("firstName", "Dave", "lastName", "Wilson", "age", 40)
      );

      McpSchema.CallToolResult result = handler.execute(null, arguments);

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("success");
      assertThat(text).contains("Dave");
      assertThat(text).contains("Wilson");
   }



   /*******************************************************************************
    ** Test inserting without a record argument returns an error.
    *******************************************************************************/
   @Test
   void testInsertWithoutRecord()
   {
      InsertToolHandler    handler = new InsertToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("record is required");
   }



   /*******************************************************************************
    ** Test updating an existing record.
    *******************************************************************************/
   @Test
   void testUpdate()
   {
      UpdateToolHandler handler = new UpdateToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);

      Map<String, Object> arguments = Map.of(
         "id", 1,
         "record", Map.of("firstName", "Alicia")
      );

      McpSchema.CallToolResult result = handler.execute(null, arguments);

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("success");

      // Verify the update took effect
      GetToolHandler       getHandler = new GetToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult getResult  = getHandler.execute(null, Map.of("id", 1));
      String getText = ((McpSchema.TextContent) getResult.content().get(0)).text();
      assertThat(getText).contains("Alicia");
   }



   /*******************************************************************************
    ** Test updating without an id returns an error.
    *******************************************************************************/
   @Test
   void testUpdateWithoutId()
   {
      UpdateToolHandler    handler = new UpdateToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of("record", Map.of("firstName", "X")));

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("id");
      assertThat(text).contains("required");
   }



   /*******************************************************************************
    ** Test deleting a record.
    *******************************************************************************/
   @Test
   void testDelete()
   {
      DeleteToolHandler    handler = new DeleteToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of("id", 1));

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("success");

      // Verify the record is gone
      GetToolHandler       getHandler = new GetToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult getResult  = getHandler.execute(null, Map.of("id", 1));
      String getText = ((McpSchema.TextContent) getResult.content().get(0)).text();
      assertThat(getText).contains("not found");
   }



   /*******************************************************************************
    ** Test deleting without an id returns an error.
    *******************************************************************************/
   @Test
   void testDeleteWithoutId()
   {
      DeleteToolHandler    handler = new DeleteToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), TestUtils.TABLE_PERSON);
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("id");
      assertThat(text).contains("required");
   }



   /*******************************************************************************
    ** Test listing tables returns the person table.
    *******************************************************************************/
   @Test
   void testListTables()
   {
      ListTablesToolHandler handler = new ListTablesToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("person");
      assertThat(text).contains("Person");
      assertThat(text).contains("\"count\" : 1");
   }



   /*******************************************************************************
    ** Test describing a table returns its field definitions.
    *******************************************************************************/
   @Test
   void testDescribeTable()
   {
      TableSchemaToolHandler handler = new TableSchemaToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());
      McpSchema.CallToolResult result  = handler.execute(null, Map.of("tableName", "person"));

      assertThat(result).isNotNull();
      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("\"tableName\" : \"person\"");
      assertThat(text).contains("firstName");
      assertThat(text).contains("lastName");
      assertThat(text).contains("email");
      assertThat(text).contains("age");
      assertThat(text).contains("INTEGER");
      assertThat(text).contains("STRING");
   }



   /*******************************************************************************
    ** Test describing a non-existent table returns an error.
    *******************************************************************************/
   @Test
   void testDescribeTableNotFound()
   {
      TableSchemaToolHandler handler = new TableSchemaToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());
      McpSchema.CallToolResult result  = handler.execute(null, Map.of("tableName", "nonExistentTable"));

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("error");
      assertThat(text).contains("not found");
   }



   /*******************************************************************************
    ** Test describing a table without providing tableName.
    *******************************************************************************/
   @Test
   void testDescribeTableWithoutName()
   {
      TableSchemaToolHandler handler = new TableSchemaToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());
      McpSchema.CallToolResult result  = handler.execute(null, Map.of());

      assertThat(result.isError()).isFalse();
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      assertThat(text).contains("tableName is required");
   }

}
