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


import java.util.Set;
import com.kingsrook.qbits.middlewaremcp.model.MCPTableMetaData;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Which tables the CURRENT session may even learn about through MCP.  A table is
 ** discoverable only if the server exposes it (allow list, not hidden, not
 ** excluded) AND the session's own permissions let it see the table (the same
 ** decision the application's metadata makes for that session), so listing and
 ** describing never disclose a table the caller could not open.  Must be called
 ** with the caller's QContext in place.
 *******************************************************************************/
public final class TableAccess
{
   /***************************************************************************
    ** Utility class.
    ***************************************************************************/
   private TableAccess()
   {
   }



   /***************************************************************************
    ** The table names the current session may see, from the application's own
    ** metadata for that session.
    ***************************************************************************/
   public static Set<String> visibleToSession() throws QException
   {
      return new MetaDataAction().execute(new MetaDataInput()).getTables().keySet();
   }



   /***************************************************************************
    ** Whether the table is exposed by this server (independent of the caller).
    ***************************************************************************/
   public static boolean isExposed(QInstance qInstance, QMCPMetaData mcpMetaData, String tableName)
   {
      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null || Boolean.TRUE.equals(table.getIsHidden()))
      {
         return false;
      }
      Set<String> exposed = mcpMetaData.getExposedTableNames();
      if(exposed != null && !exposed.contains(tableName))
      {
         return false;
      }
      MCPTableMetaData supplemental = (MCPTableMetaData) table.getSupplementalMetaData(MCPTableMetaData.TYPE);
      return supplemental == null || !supplemental.isExcluded();
   }



   /***************************************************************************
    ** Exposed by this server and visible to the current session.
    ***************************************************************************/
   public static boolean isDiscoverable(QInstance qInstance, QMCPMetaData mcpMetaData, String tableName) throws QException
   {
      return isExposed(qInstance, mcpMetaData, tableName) && visibleToSession().contains(tableName);
   }
}
