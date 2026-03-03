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

package com.kingsrook.qbits.middlewaremcp.session;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import io.modelcontextprotocol.server.McpSyncServerExchange;


/*******************************************************************************
 ** Strategy interface for obtaining a QSession when an MCP client connects.
 *******************************************************************************/
public interface MCPSessionProviderInterface
{

   /*******************************************************************************
    ** Called once per tool/resource invocation.
    ** The exchange parameter gives access to client info and transport details.
    ** Return a valid QSession, or throw if the connection should be rejected.
    *******************************************************************************/
   QSession createSession(McpSyncServerExchange exchange) throws QException;

}
