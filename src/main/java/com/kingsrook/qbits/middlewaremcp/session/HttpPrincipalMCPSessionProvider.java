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


import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;


/*******************************************************************************
 ** The session of an MCP call is the QQQ session the caller authenticated as on
 ** THIS HTTP request (see {@link com.kingsrook.qbits.middlewaremcp.transport.McpBearerAuthenticationFilter}),
 ** never a server-wide identity: each call runs as exactly its own caller, under
 ** that caller's permissions, security keys (tenant) and revocation state.  A call
 ** without an authenticated principal, or with a system session, is refused.
 *******************************************************************************/
public class HttpPrincipalMCPSessionProvider implements MCPSessionProviderInterface
{
   /** Key of the authenticated QSession in the per-request transport context. */
   public static final String CONTEXT_KEY = "com.kingsrook.qbits.middlewaremcp.session";



   /***************************************************************************
    ** The calling principal's session, or a refusal.
    ***************************************************************************/
   @Override
   public QSession createSession(McpSyncServerExchange exchange) throws QException
   {
      McpTransportContext context = exchange == null ? null : exchange.transportContext();
      Object              value   = context == null ? null : context.get(CONTEXT_KEY);
      if(!(value instanceof QSession session) || session instanceof QSystemUserSession || session.getUser() == null)
      {
         throw new QAuthenticationException("No authenticated principal for this MCP request");
      }
      return session;
   }
}
