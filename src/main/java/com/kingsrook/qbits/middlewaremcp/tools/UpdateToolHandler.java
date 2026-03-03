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
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "update_{tableName}" — updates an existing record by primary key.
 ** This is an opt-in operation (only registered when UPDATE is in allowedOperations).
 *******************************************************************************/
public class UpdateToolHandler extends AbstractQQQToolHandler
{
   private final String tableName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, String tableName)
   {
      super(qInstance, mcpMetaData, sessionProvider);
      this.tableName = tableName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   @SuppressWarnings("unchecked")
   protected Object doExecute(Map<String, Object> arguments) throws QException
   {
      Object idArg = arguments.get("id");
      if(idArg == null)
      {
         return Map.of("error", "id (primary key) is required");
      }

      Object recordArg = arguments.get("record");
      if(!(recordArg instanceof Map))
      {
         return Map.of("error", "record is required and must be an object mapping field names to values");
      }

      Map<String, Object> recordMap = (Map<String, Object>) recordArg;

      // Determine the primary key field
      QTableMetaData table      = qInstance.getTable(tableName);
      String         pkField    = table != null ? table.getPrimaryKeyField() : "id";
      QRecord        record     = new QRecord();
      record.setTableName(tableName);

      // Set the primary key
      if(idArg instanceof Serializable)
      {
         record.setValue(pkField, (Serializable) idArg);
      }
      else
      {
         record.setValue(pkField, String.valueOf(idArg));
      }

      // Set the other fields
      for(Map.Entry<String, Object> entry : recordMap.entrySet())
      {
         Object value = entry.getValue();
         if(value instanceof Serializable || value == null)
         {
            record.setValue(entry.getKey(), (Serializable) value);
         }
         else
         {
            record.setValue(entry.getKey(), String.valueOf(value));
         }
      }

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(tableName);
      updateInput.setRecords(List.of(record));

      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

      Map<String, Object> result = new LinkedHashMap<>();
      if(updateOutput.getRecords() != null && !updateOutput.getRecords().isEmpty())
      {
         QRecord updated = updateOutput.getRecords().get(0);
         if(updated.getErrors() != null && !updated.getErrors().isEmpty())
         {
            result.put("error", "Update failed: " + updated.getErrors().get(0).getMessage());
         }
         else
         {
            result.put("success", true);
            result.put("record", recordToMap(updated));
         }
      }
      else
      {
         result.put("error", "Update failed: no record returned");
      }

      return result;
   }

}
