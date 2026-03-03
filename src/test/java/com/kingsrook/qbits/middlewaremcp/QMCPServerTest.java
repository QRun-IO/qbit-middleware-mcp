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
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.model.MCPOperation;
import com.kingsrook.qbits.middlewaremcp.session.AnonymousMCPSessionProvider;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Tests for QMCPServer — verifies that the server builds correctly with
 ** various configurations.
 *******************************************************************************/
class QMCPServerTest
{

   /*******************************************************************************
    ** Verify that a server builds successfully with minimal configuration.
    *******************************************************************************/
   @Test
   void testBuildWithMinimalConfig()
   {
      QInstance    qInstance   = TestUtils.defineInstance();
      QMCPMetaData mcpMetaData = TestUtils.defineMCPMetaData();

      McpSyncServer server = QMCPServer.build(qInstance, mcpMetaData, new StdioServerTransportProvider());
      assertThat(server).isNotNull();
      server.close();
   }



   /*******************************************************************************
    ** Verify that a server with exposedTableNames only registers specified tables.
    *******************************************************************************/
   @Test
   void testBuildWithExposedTableNames()
   {
      QInstance qInstance = TestUtils.defineInstance();

      QMCPMetaData mcpMetaData = new QMCPMetaData()
         .withSessionProvider(new AnonymousMCPSessionProvider())
         .withExposedTableNames(Set.of(TestUtils.TABLE_PERSON))
         .withDefaultTableOperations(EnumSet.of(MCPOperation.QUERY, MCPOperation.COUNT, MCPOperation.GET));

      McpSyncServer server = QMCPServer.build(qInstance, mcpMetaData, new StdioServerTransportProvider());
      assertThat(server).isNotNull();
      server.close();
   }



   /*******************************************************************************
    ** Verify that missing sessionProvider throws an IllegalArgumentException.
    *******************************************************************************/
   @Test
   void testBuildWithoutSessionProviderThrows()
   {
      QInstance    qInstance   = TestUtils.defineInstance();
      QMCPMetaData mcpMetaData = new QMCPMetaData();
      // No sessionProvider set

      assertThatThrownBy(() -> QMCPServer.build(qInstance, mcpMetaData, new StdioServerTransportProvider()))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("sessionProvider");
   }



   /*******************************************************************************
    ** Verify server identity fields are applied correctly.
    *******************************************************************************/
   @Test
   void testServerIdentity()
   {
      QInstance    qInstance   = TestUtils.defineInstance();
      QMCPMetaData mcpMetaData = TestUtils.defineMCPMetaData()
         .withServerName("my-test-app")
         .withServerVersion("2.0.0");

      assertThat(mcpMetaData.getServerName()).isEqualTo("my-test-app");
      assertThat(mcpMetaData.getServerVersion()).isEqualTo("2.0.0");

      McpSyncServer server = QMCPServer.build(qInstance, mcpMetaData, new StdioServerTransportProvider());
      assertThat(server).isNotNull();
      server.close();
   }

}
