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

package com.kingsrook.qbits.middlewaremcp;


import java.util.EnumSet;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qbits.middlewaremcp.model.MCPOperation;
import com.kingsrook.qbits.middlewaremcp.session.FixedUserMCPSessionProvider;


/*******************************************************************************
 ** Test utilities for the qqq-middleware-mcp module.
 *******************************************************************************/
public class TestUtils
{
   public static final String BACKEND_NAME = "memory";
   public static final String TABLE_PERSON = "person";



   /*******************************************************************************
    ** Build a minimal QInstance with an in-memory backend and a person table.
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.setAuthentication(new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.FULLY_ANONYMOUS));

      QBackendMetaData backend = new QBackendMetaData()
         .withName(BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class);
      qInstance.addBackend(backend);

      qInstance.addTable(definePersonTable());

      return qInstance;
   }



   /*******************************************************************************
    ** Define a simple person table for testing.
    *******************************************************************************/
   public static QTableMetaData definePersonTable()
   {
      return new QTableMetaData()
         .withName(TABLE_PERSON)
         .withLabel("Person")
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING).withLabel("First Name").withIsRequired(true))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING).withLabel("Last Name").withIsRequired(true))
         .withField(new QFieldMetaData("email", QFieldType.STRING).withLabel("Email"))
         .withField(new QFieldMetaData("age", QFieldType.INTEGER).withLabel("Age"))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE).withLabel("Birth Date"));
   }



   /*******************************************************************************
    ** Build a QMCPMetaData configured for tests.
    *******************************************************************************/
   public static QMCPMetaData defineMCPMetaData()
   {
      return new QMCPMetaData()
         .withSessionProvider(new FixedUserMCPSessionProvider(new QSession()))
         .withDefaultTableOperations(EnumSet.allOf(MCPOperation.class))
         .withServerName("test-mcp-server");
   }

}
