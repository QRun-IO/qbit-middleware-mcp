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

package com.kingsrook.qbits.middlewaremcp.transport;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qbits.middlewaremcp.session.HttpPrincipalMCPSessionProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/*******************************************************************************
 ** Authenticates EVERY MCP HTTP request (initialize, list, read, call, notifications
 ** and stream opens alike) with the application's own authentication module, from
 ** an <code>Authorization: Bearer</code> token only: no cookies, API keys, basic
 ** credentials or query parameters are accepted here.  The resulting QSession is
 ** carried to the tool handlers in the per-request transport context
 ** ({@link #extractor()}), so the MCP server holds no identity of its own.  A
 ** request that cannot authenticate is answered 401 and never reaches the MCP
 ** server; an MCP session id does not substitute for credentials, because the
 ** token is checked again on every request.
 *******************************************************************************/
public class McpBearerAuthenticationFilter implements Filter
{
   private static final QLogger LOG = QLogger.getLogger(McpBearerAuthenticationFilter.class);

   private static final String REQUEST_ATTRIBUTE = McpBearerAuthenticationFilter.class.getName() + ".session";
   private static final String BEARER_PREFIX     = "Bearer ";

   private final QInstance qInstance;



   /***************************************************************************
    ** Bind to the application whose authentication is used.  Authentication
    ** must exist and permissions must be enforced in core actions, otherwise
    ** the tools would run unprotected.
    ***************************************************************************/
   public McpBearerAuthenticationFilter(QInstance qInstance)
   {
      if(qInstance == null || qInstance.getAuthentication() == null)
      {
         throw new IllegalArgumentException("MCP over HTTP needs an application authentication provider");
      }
      if(!Boolean.TRUE.equals(qInstance.getEnforcePermissionsInCoreActions()))
      {
         throw new IllegalArgumentException("MCP over HTTP requires permission enforcement in core actions");
      }
      this.qInstance = qInstance;
   }



   /***************************************************************************
    ** Authenticate, then continue; or answer 401.
    ***************************************************************************/
   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
   {
      HttpServletRequest  http   = (HttpServletRequest) request;
      HttpServletResponse answer = (HttpServletResponse) response;

      String header = http.getHeader("Authorization");
      if(header == null || !header.startsWith(BEARER_PREFIX) || header.length() == BEARER_PREFIX.length())
      {
         reject(answer);
         return;
      }

      QSession session = null;
      try
      {
         QAuthenticationModuleInterface module  = new QAuthenticationModuleDispatcher().getQModule(qInstance.getAuthentication());
         Map<String, String>            context = new HashMap<>();
         context.put("accessToken", header.substring(BEARER_PREFIX.length()).trim());
         QContext.init(qInstance, null);
         session = module.createSession(qInstance, qInstance.getAuthentication(), context);
      }
      catch(Exception e)
      {
         LOG.info("MCP request authentication failed", e);
      }
      finally
      {
         QContext.clear();
      }

      if(session == null || session instanceof QSystemUserSession || session.getUser() == null)
      {
         reject(answer);
         return;
      }
      request.setAttribute(REQUEST_ATTRIBUTE, session);
      chain.doFilter(request, response);
   }



   /***************************************************************************
    ** 401 with a challenge and no detail.
    ***************************************************************************/
   private static void reject(HttpServletResponse response) throws IOException
   {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setHeader("WWW-Authenticate", "Bearer");
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"unauthorized\"}");
   }



   /***************************************************************************
    ** The extractor that hands the request's authenticated session to the MCP
    ** server, for use when building the transport.
    ***************************************************************************/
   public static McpTransportContextExtractor<HttpServletRequest> extractor()
   {
      return request ->
      {
         Object session = request.getAttribute(REQUEST_ATTRIBUTE);
         return session == null ? McpTransportContext.EMPTY : McpTransportContext.create(Map.of(HttpPrincipalMCPSessionProvider.CONTEXT_KEY, session));
      };
   }
}
