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
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "get_{tableName}" — retrieves a single record by primary key.
 *******************************************************************************/
public class GetToolHandler extends AbstractQQQToolHandler
{
   private final String tableName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public GetToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, String tableName)
   {
      super(qInstance, mcpMetaData, sessionProvider);
      this.tableName = tableName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected Object doExecute(Map<String, Object> arguments) throws QException
   {
      Object primaryKey = arguments.get("id");
      if(primaryKey == null)
      {
         return Map.of("error", "id is required");
      }

      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      getInput.setPrimaryKey((Serializable) primaryKey);
      getInput.setShouldGenerateDisplayValues(true);
      getInput.setShouldTranslatePossibleValues(true);

      GetOutput getOutput = new GetAction().execute(getInput);
      QRecord   record    = getOutput.getRecord();

      if(record == null)
      {
         return Map.of("error", "Record not found with id: " + primaryKey);
      }

      return recordToMap(record);
   }

}
