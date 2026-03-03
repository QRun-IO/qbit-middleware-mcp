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


import com.kingsrook.qqq.backend.core.model.session.QSession;
import io.modelcontextprotocol.server.McpSyncServerExchange;


/*******************************************************************************
 ** Session provider that returns an empty (anonymous) QSession.
 ** The QQQ permission model determines what data is accessible.
 ** Suitable for read-only public data scenarios.
 *******************************************************************************/
public class AnonymousMCPSessionProvider implements MCPSessionProviderInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSession createSession(McpSyncServerExchange exchange)
   {
      return new QSession();
   }

}
