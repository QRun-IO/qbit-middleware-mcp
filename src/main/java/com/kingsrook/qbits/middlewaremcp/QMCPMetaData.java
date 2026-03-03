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
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qbits.middlewaremcp.model.MCPOperation;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Instance-level MCP configuration. Attach to a QInstance to configure the
 ** MCP server's behavior — which tables to expose, which operations to allow,
 ** and how to authenticate MCP clients.
 *******************************************************************************/
public class QMCPMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "mcp";

   // Which tables to expose — null means "all readable tables"
   private Set<String> exposedTableNames;

   // Which processes to expose — null means "none" (safe default)
   private Set<String> exposedProcessNames;

   // Operations allowed globally (per-table can further restrict or expand)
   // Default: {QUERY, COUNT, GET} — read-only
   private Set<MCPOperation> defaultTableOperations = EnumSet.of(MCPOperation.QUERY, MCPOperation.COUNT, MCPOperation.GET);

   // How to obtain a QSession for an MCP connection — required
   private MCPSessionProviderInterface sessionProvider;

   // Max records returned from a single query (default 100, hard ceiling 1000)
   private int defaultQueryLimit = 100;
   private int maxQueryLimit     = 1000;

   // MCP server identity sent in the initialize handshake
   private String serverName    = "qqq-mcp-server";
   private String serverVersion = "1.0.0";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return NAME;
   }



   /*******************************************************************************
    ** Retrieve a QMCPMetaData from a QInstance.
    *******************************************************************************/
   public static QMCPMetaData of(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.of(qInstance, NAME);
   }



   /*******************************************************************************
    ** Getter for exposedTableNames
    *******************************************************************************/
   public Set<String> getExposedTableNames()
   {
      return exposedTableNames;
   }



   /*******************************************************************************
    ** Setter for exposedTableNames
    *******************************************************************************/
   public void setExposedTableNames(Set<String> exposedTableNames)
   {
      this.exposedTableNames = exposedTableNames;
   }



   /*******************************************************************************
    ** Fluent setter for exposedTableNames
    *******************************************************************************/
   public QMCPMetaData withExposedTableNames(Set<String> exposedTableNames)
   {
      this.exposedTableNames = exposedTableNames;
      return this;
   }



   /*******************************************************************************
    ** Getter for exposedProcessNames
    *******************************************************************************/
   public Set<String> getExposedProcessNames()
   {
      return exposedProcessNames;
   }



   /*******************************************************************************
    ** Setter for exposedProcessNames
    *******************************************************************************/
   public void setExposedProcessNames(Set<String> exposedProcessNames)
   {
      this.exposedProcessNames = exposedProcessNames;
   }



   /*******************************************************************************
    ** Fluent setter for exposedProcessNames
    *******************************************************************************/
   public QMCPMetaData withExposedProcessNames(Set<String> exposedProcessNames)
   {
      this.exposedProcessNames = exposedProcessNames;
      return this;
   }



   /*******************************************************************************
    ** Getter for defaultTableOperations
    *******************************************************************************/
   public Set<MCPOperation> getDefaultTableOperations()
   {
      return defaultTableOperations;
   }



   /*******************************************************************************
    ** Setter for defaultTableOperations
    *******************************************************************************/
   public void setDefaultTableOperations(Set<MCPOperation> defaultTableOperations)
   {
      this.defaultTableOperations = defaultTableOperations;
   }



   /*******************************************************************************
    ** Fluent setter for defaultTableOperations
    *******************************************************************************/
   public QMCPMetaData withDefaultTableOperations(Set<MCPOperation> defaultTableOperations)
   {
      this.defaultTableOperations = defaultTableOperations;
      return this;
   }



   /*******************************************************************************
    ** Getter for sessionProvider
    *******************************************************************************/
   public MCPSessionProviderInterface getSessionProvider()
   {
      return sessionProvider;
   }



   /*******************************************************************************
    ** Setter for sessionProvider
    *******************************************************************************/
   public void setSessionProvider(MCPSessionProviderInterface sessionProvider)
   {
      this.sessionProvider = sessionProvider;
   }



   /*******************************************************************************
    ** Fluent setter for sessionProvider
    *******************************************************************************/
   public QMCPMetaData withSessionProvider(MCPSessionProviderInterface sessionProvider)
   {
      this.sessionProvider = sessionProvider;
      return this;
   }



   /*******************************************************************************
    ** Getter for defaultQueryLimit
    *******************************************************************************/
   public int getDefaultQueryLimit()
   {
      return defaultQueryLimit;
   }



   /*******************************************************************************
    ** Setter for defaultQueryLimit
    *******************************************************************************/
   public void setDefaultQueryLimit(int defaultQueryLimit)
   {
      this.defaultQueryLimit = defaultQueryLimit;
   }



   /*******************************************************************************
    ** Fluent setter for defaultQueryLimit
    *******************************************************************************/
   public QMCPMetaData withDefaultQueryLimit(int defaultQueryLimit)
   {
      this.defaultQueryLimit = defaultQueryLimit;
      return this;
   }



   /*******************************************************************************
    ** Getter for maxQueryLimit
    *******************************************************************************/
   public int getMaxQueryLimit()
   {
      return maxQueryLimit;
   }



   /*******************************************************************************
    ** Setter for maxQueryLimit
    *******************************************************************************/
   public void setMaxQueryLimit(int maxQueryLimit)
   {
      this.maxQueryLimit = maxQueryLimit;
   }



   /*******************************************************************************
    ** Fluent setter for maxQueryLimit
    *******************************************************************************/
   public QMCPMetaData withMaxQueryLimit(int maxQueryLimit)
   {
      this.maxQueryLimit = maxQueryLimit;
      return this;
   }



   /*******************************************************************************
    ** Getter for serverName
    *******************************************************************************/
   public String getServerName()
   {
      return serverName;
   }



   /*******************************************************************************
    ** Setter for serverName
    *******************************************************************************/
   public void setServerName(String serverName)
   {
      this.serverName = serverName;
   }



   /*******************************************************************************
    ** Fluent setter for serverName
    *******************************************************************************/
   public QMCPMetaData withServerName(String serverName)
   {
      this.serverName = serverName;
      return this;
   }



   /*******************************************************************************
    ** Getter for serverVersion
    *******************************************************************************/
   public String getServerVersion()
   {
      return serverVersion;
   }



   /*******************************************************************************
    ** Setter for serverVersion
    *******************************************************************************/
   public void setServerVersion(String serverVersion)
   {
      this.serverVersion = serverVersion;
   }



   /*******************************************************************************
    ** Fluent setter for serverVersion
    *******************************************************************************/
   public QMCPMetaData withServerVersion(String serverVersion)
   {
      this.serverVersion = serverVersion;
      return this;
   }

}
