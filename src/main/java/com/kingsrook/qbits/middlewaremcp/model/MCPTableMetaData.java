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

package com.kingsrook.qbits.middlewaremcp.model;


import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;


/*******************************************************************************
 ** Per-table supplemental metadata for MCP configuration.
 ** Attached to QTableMetaData to customize how a table is exposed via MCP.
 *******************************************************************************/
public class MCPTableMetaData extends QSupplementalTableMetaData
{
   public static final String TYPE = "mcp";

   // Fields to expose — null means all (excluding hidden fields)
   private Set<String> exposedFieldNames;

   // Override for operations on this table (overrides QMCPMetaData defaults)
   private Set<MCPOperation> allowedOperations;

   // Richer natural-language description for the AI (supplements table label)
   private String aiDescription;

   // If true, this table is excluded entirely from MCP exposure
   private boolean excluded = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return TYPE;
   }



   /*******************************************************************************
    ** Getter for exposedFieldNames
    *******************************************************************************/
   public Set<String> getExposedFieldNames()
   {
      return exposedFieldNames;
   }



   /*******************************************************************************
    ** Setter for exposedFieldNames
    *******************************************************************************/
   public void setExposedFieldNames(Set<String> exposedFieldNames)
   {
      this.exposedFieldNames = exposedFieldNames;
   }



   /*******************************************************************************
    ** Fluent setter for exposedFieldNames
    *******************************************************************************/
   public MCPTableMetaData withExposedFieldNames(Set<String> exposedFieldNames)
   {
      this.exposedFieldNames = exposedFieldNames;
      return this;
   }



   /*******************************************************************************
    ** Getter for allowedOperations
    *******************************************************************************/
   public Set<MCPOperation> getAllowedOperations()
   {
      return allowedOperations;
   }



   /*******************************************************************************
    ** Setter for allowedOperations
    *******************************************************************************/
   public void setAllowedOperations(Set<MCPOperation> allowedOperations)
   {
      this.allowedOperations = allowedOperations;
   }



   /*******************************************************************************
    ** Fluent setter for allowedOperations
    *******************************************************************************/
   public MCPTableMetaData withAllowedOperations(Set<MCPOperation> allowedOperations)
   {
      this.allowedOperations = allowedOperations;
      return this;
   }



   /*******************************************************************************
    ** Getter for aiDescription
    *******************************************************************************/
   public String getAiDescription()
   {
      return aiDescription;
   }



   /*******************************************************************************
    ** Setter for aiDescription
    *******************************************************************************/
   public void setAiDescription(String aiDescription)
   {
      this.aiDescription = aiDescription;
   }



   /*******************************************************************************
    ** Fluent setter for aiDescription
    *******************************************************************************/
   public MCPTableMetaData withAiDescription(String aiDescription)
   {
      this.aiDescription = aiDescription;
      return this;
   }



   /*******************************************************************************
    ** Getter for excluded
    *******************************************************************************/
   public boolean isExcluded()
   {
      return excluded;
   }



   /*******************************************************************************
    ** Setter for excluded
    *******************************************************************************/
   public void setExcluded(boolean excluded)
   {
      this.excluded = excluded;
   }



   /*******************************************************************************
    ** Fluent setter for excluded
    *******************************************************************************/
   public MCPTableMetaData withExcluded(boolean excluded)
   {
      this.excluded = excluded;
      return this;
   }

}
