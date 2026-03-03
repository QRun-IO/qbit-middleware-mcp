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
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.model.MCPTableMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "list_tables" — enumerates all MCP-exposed tables with
 ** their labels and descriptions.
 *******************************************************************************/
public class ListTablesToolHandler extends AbstractQQQToolHandler
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public ListTablesToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider)
   {
      super(qInstance, mcpMetaData, sessionProvider);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected Object doExecute(Map<String, Object> arguments) throws QException
   {
      Set<String>         exposedTableNames = mcpMetaData.getExposedTableNames();
      List<Map<String, Object>> tables      = new ArrayList<>();

      for(Map.Entry<String, QTableMetaData> entry : qInstance.getTables().entrySet())
      {
         String         tableName = entry.getKey();
         QTableMetaData table     = entry.getValue();

         // Skip hidden tables
         if(Boolean.TRUE.equals(table.getIsHidden()))
         {
            continue;
         }

         // Filter to exposed tables if specified
         if(exposedTableNames != null && !exposedTableNames.contains(tableName))
         {
            continue;
         }

         // Check if table is excluded via MCPTableMetaData
         MCPTableMetaData mcpTableMetaData = (MCPTableMetaData) table.getSupplementalMetaData(MCPTableMetaData.TYPE);
         if(mcpTableMetaData != null && mcpTableMetaData.isExcluded())
         {
            continue;
         }

         Map<String, Object> tableInfo = new LinkedHashMap<>();
         tableInfo.put("name", tableName);
         tableInfo.put("label", table.getLabel() != null ? table.getLabel() : tableName);

         String description = "";
         if(mcpTableMetaData != null && mcpTableMetaData.getAiDescription() != null)
         {
            description = mcpTableMetaData.getAiDescription();
         }
         tableInfo.put("description", description);
         tableInfo.put("primaryKeyField", table.getPrimaryKeyField());

         tables.add(tableInfo);
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("tables", tables);
      result.put("count", tables.size());
      return result;
   }

}
