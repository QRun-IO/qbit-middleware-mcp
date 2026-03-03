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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.model.MCPTableMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "describe_table" — describes a table's fields, types, and
 ** relationships so the AI can construct valid queries and mutations.
 *******************************************************************************/
public class TableSchemaToolHandler extends AbstractQQQToolHandler
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public TableSchemaToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider)
   {
      super(qInstance, mcpMetaData, sessionProvider);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected Object doExecute(Map<String, Object> arguments) throws QException
   {
      String tableName = getStringArg(arguments, "tableName");
      if(tableName == null || tableName.isEmpty())
      {
         return Map.of("error", "tableName is required");
      }

      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null)
      {
         return Map.of("error", "Table not found: " + tableName);
      }

      MCPTableMetaData mcpTableMetaData = (MCPTableMetaData) table.getSupplementalMetaData(MCPTableMetaData.TYPE);
      Set<String>      exposedFields    = mcpTableMetaData != null ? mcpTableMetaData.getExposedFieldNames() : null;

      List<Map<String, Object>> fields = new ArrayList<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         // Skip hidden fields
         if(field.getIsHidden())
         {
            continue;
         }

         // Filter to exposed fields if specified
         if(exposedFields != null && !exposedFields.contains(field.getName()))
         {
            continue;
         }

         Map<String, Object> fieldInfo = new LinkedHashMap<>();
         fieldInfo.put("name", field.getName());
         fieldInfo.put("label", field.getLabel() != null ? field.getLabel() : field.getName());
         fieldInfo.put("type", field.getType() != null ? field.getType().name() : "STRING");
         fieldInfo.put("isRequired", field.getIsRequired());
         fieldInfo.put("isPrimaryKey", field.getName().equals(table.getPrimaryKeyField()));

         if(field.getDisplayFormat() != null)
         {
            fieldInfo.put("displayFormat", field.getDisplayFormat());
         }

         if(field.getPossibleValueSourceName() != null)
         {
            fieldInfo.put("possibleValueSource", field.getPossibleValueSourceName());
         }

         if(field.getDefaultValue() != null)
         {
            fieldInfo.put("defaultValue", String.valueOf(field.getDefaultValue()));
         }

         fields.add(fieldInfo);
      }

      String description = "";
      if(mcpTableMetaData != null && mcpTableMetaData.getAiDescription() != null)
      {
         description = mcpTableMetaData.getAiDescription();
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("tableName", tableName);
      result.put("label", table.getLabel() != null ? table.getLabel() : tableName);
      result.put("description", description);
      result.put("primaryKeyField", table.getPrimaryKeyField());
      result.put("fields", fields);
      return result;
   }

}
