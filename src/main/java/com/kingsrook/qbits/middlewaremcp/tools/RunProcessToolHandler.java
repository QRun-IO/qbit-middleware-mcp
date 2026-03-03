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


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "run_process_{processName}" — executes a QQQ process.
 ** This is an opt-in operation (only registered for explicitly exposed processes).
 *******************************************************************************/
public class RunProcessToolHandler extends AbstractQQQToolHandler
{
   private final String processName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, String processName)
   {
      super(qInstance, mcpMetaData, sessionProvider);
      this.processName = processName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected Object doExecute(Map<String, Object> arguments) throws QException
   {
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(processName);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);

      // Pass all arguments as process values
      for(Map.Entry<String, Object> entry : arguments.entrySet())
      {
         Object value = entry.getValue();
         if(value instanceof Serializable)
         {
            runProcessInput.addValue(entry.getKey(), (Serializable) value);
         }
         else if(value != null)
         {
            runProcessInput.addValue(entry.getKey(), String.valueOf(value));
         }
      }

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("processName", processName);
      result.put("processUUID", runProcessOutput.getProcessUUID());

      if(runProcessOutput.getException().isPresent())
      {
         result.put("error", runProcessOutput.getException().get().getMessage());
      }
      else
      {
         result.put("success", true);

         // Include process output values
         if(runProcessOutput.getValues() != null && !runProcessOutput.getValues().isEmpty())
         {
            Map<String, Object> outputValues = new LinkedHashMap<>();
            for(Map.Entry<String, Serializable> entry : runProcessOutput.getValues().entrySet())
            {
               outputValues.put(entry.getKey(), entry.getValue());
            }
            result.put("outputValues", outputValues);
         }

         if(runProcessOutput.getRecords() != null && !runProcessOutput.getRecords().isEmpty())
         {
            result.put("records", recordsToList(runProcessOutput.getRecords()));
         }
      }

      return result;
   }

}
