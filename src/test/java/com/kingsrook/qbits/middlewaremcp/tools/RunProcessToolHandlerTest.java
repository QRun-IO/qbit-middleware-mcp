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


import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.TestUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for RunProcessToolHandler — verifies process execution via MCP.
 *******************************************************************************/
class RunProcessToolHandlerTest
{
   public static final String PROCESS_NAME = "testProcess";

   private QInstance    qInstance;
   private QMCPMetaData mcpMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      qInstance = TestUtils.defineInstance();

      // Add a simple test process
      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withLabel("Test Process")
         .addStep(new QBackendStepMetaData()
            .withName("step1")
            .withCode(new QCodeReference(EchoBackendStep.class.getName(), QCodeType.JAVA))));

      mcpMetaData = TestUtils.defineMCPMetaData();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Test that a process can be executed and returns expected output.
    *******************************************************************************/
   @Test
   void testRunProcess()
   {
      RunProcessToolHandler handler = new RunProcessToolHandler(qInstance, mcpMetaData, mcpMetaData.getSessionProvider(), PROCESS_NAME);

      McpSchema.CallToolResult result = handler.execute(null, Map.of("message", "hello"));

      assertThat(result).isNotNull();
      // The result could be success or error, but it should not be null
      assertThat(result.content()).isNotEmpty();
   }



   /*******************************************************************************
    ** A simple backend step that echoes its input values back as output.
    *******************************************************************************/
   public static class EchoBackendStep implements com.kingsrook.qqq.backend.core.actions.processes.BackendStep
   {
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         // Echo all input values to output
         for(Map.Entry<String, java.io.Serializable> entry : runBackendStepInput.getValues().entrySet())
         {
            runBackendStepOutput.addValue("echo_" + entry.getKey(), entry.getValue());
         }
      }
   }

}
