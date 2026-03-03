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

package com.kingsrook.qbits.middlewaremcp.resources;


import java.util.List;
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
 ** Tests for TableListResourceHandler and TableSchemaResourceHandler.
 *******************************************************************************/
class ResourceHandlerTest
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

      // Insert a test record for record resource tests
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Alice").withValue("lastName", "Smith").withValue("age", 30)
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
    ** Test that the table list resource returns all exposed tables.
    *******************************************************************************/
   @Test
   void testTableListResource()
   {
      TableListResourceHandler handler = new TableListResourceHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());

      McpSchema.ReadResourceResult result = handler.handle(null,
         new McpSchema.ReadResourceRequest("qqq://tables"));

      assertThat(result).isNotNull();
      assertThat(result.contents()).hasSize(1);

      McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().get(0);
      assertThat(contents.uri()).isEqualTo("qqq://tables");
      assertThat(contents.mimeType()).isEqualTo("application/json");
      assertThat(contents.text()).contains("person");
      assertThat(contents.text()).contains("Person");
      assertThat(contents.text()).contains("\"count\" : 1");
   }



   /*******************************************************************************
    ** Test that the table schema resource returns field definitions.
    *******************************************************************************/
   @Test
   void testTableSchemaResource()
   {
      TableSchemaResourceHandler handler = new TableSchemaResourceHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());

      McpSchema.ReadResourceResult result = handler.handle(null,
         new McpSchema.ReadResourceRequest("qqq://table/person/schema"));

      assertThat(result).isNotNull();
      assertThat(result.contents()).hasSize(1);

      McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().get(0);
      assertThat(contents.mimeType()).isEqualTo("application/json");

      String json = contents.text();
      assertThat(json).contains("\"tableName\" : \"person\"");
      assertThat(json).contains("firstName");
      assertThat(json).contains("lastName");
      assertThat(json).contains("age");
      assertThat(json).contains("INTEGER");
      assertThat(json).contains("STRING");
      assertThat(json).contains("\"primaryKeyField\" : \"id\"");
   }



   /*******************************************************************************
    ** Test that requesting a schema for a non-existent table returns an error.
    *******************************************************************************/
   @Test
   void testTableSchemaResourceNotFound()
   {
      TableSchemaResourceHandler handler = new TableSchemaResourceHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());

      McpSchema.ReadResourceResult result = handler.handle(null,
         new McpSchema.ReadResourceRequest("qqq://table/nonExistent/schema"));

      assertThat(result).isNotNull();
      McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().get(0);
      assertThat(contents.text()).contains("error");
      assertThat(contents.text()).contains("not found");
   }



   /*******************************************************************************
    ** Test that a malformed URI returns an error.
    *******************************************************************************/
   @Test
   void testTableSchemaResourceBadUri()
   {
      TableSchemaResourceHandler handler = new TableSchemaResourceHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider());

      McpSchema.ReadResourceResult result = handler.handle(null,
         new McpSchema.ReadResourceRequest("qqq://invalid/uri"));

      assertThat(result).isNotNull();
      McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().get(0);
      assertThat(contents.text()).contains("error");
   }

}
