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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "delete_{tableName}" — deletes a record by primary key.
 ** This is an opt-in operation (only registered when DELETE is in allowedOperations).
 *******************************************************************************/
public class DeleteToolHandler extends AbstractQQQToolHandler
{
   private final String tableName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public DeleteToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, String tableName)
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
      Object idArg = arguments.get("id");
      if(idArg == null)
      {
         return Map.of("error", "id (primary key) is required");
      }

      Serializable primaryKey = (idArg instanceof Serializable) ? (Serializable) idArg : String.valueOf(idArg);

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(tableName);
      deleteInput.setPrimaryKeys(List.of(primaryKey));

      DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);

      Map<String, Object> result = new LinkedHashMap<>();
      if(deleteOutput.getRecordsWithErrors() != null && !deleteOutput.getRecordsWithErrors().isEmpty())
      {
         String errorMsg = deleteOutput.getRecordsWithErrors().get(0).getErrors() != null && !deleteOutput.getRecordsWithErrors().get(0).getErrors().isEmpty()
            ? deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage()
            : "Unknown delete error";
         result.put("error", "Delete failed: " + errorMsg);
      }
      else
      {
         result.put("success", true);
         result.put("deletedRecordCount", deleteOutput.getDeletedRecordCount());
      }

      return result;
   }

}
